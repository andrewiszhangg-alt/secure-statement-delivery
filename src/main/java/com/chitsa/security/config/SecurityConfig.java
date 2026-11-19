package com.chitsa.security.config;

import com.chitsa.security.handler.CognitoLogoutSuccessHandler;
import com.chitsa.security.handler.LoggingAuthenticationFailureHandler;
import com.chitsa.security.util.AccessTokenClientValidator;
import com.chitsa.security.util.BearerAuthorizationRequestMatcher;
import com.chitsa.security.util.CognitoClaims;
import com.chitsa.security.util.CsrfCookieFilter;
import com.chitsa.security.util.SecurityConstant;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.SupplierJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.util.HashSet;
import java.util.Set;

/**
 * Two chains: {@code /api/**} takes Cognito bearer tokens (or the admin's web session plus CSRF header); the web pages use
 * a server-side session created by Authorization Code + PKCE sign-in through the Cognito Hosted UI.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(CognitoProperties.class)
public class SecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain apiChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> CognitoClaims.authorities(jwt.getClaims()));

        http.securityMatcher(SecurityConstant.API_PATH)
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole(SecurityConstant.ADMIN))
                .oauth2ResourceServer(rs -> rs.jwt(jwt -> jwt
                        .decoder(jwtDecoder)
                        .jwtAuthenticationConverter(converter)))
                // Also accepts the admin's web session (e.g. from Scalar); sessions are never created here.
                // Cookie-borne sessions need the XSRF-TOKEN value in X-XSRF-TOKEN; bearer calls are exempt.
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.NEVER))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers(new BearerAuthorizationRequestMatcher()))
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class);
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain webChain(HttpSecurity http, ClientRegistrationRepository registrations,
                                 CognitoProperties properties) throws Exception {
        var resolver = new DefaultOAuth2AuthorizationRequestResolver(
                registrations, SecurityConstant.OAUTH2_AUTHORIZATION_PATH);
        resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());

        // fetch() calls get a 401 instead of a redirect to the Cognito login page
        var jsonRequests = new MediaTypeRequestMatcher(MediaType.APPLICATION_JSON);
        jsonRequests.setIgnoredMediaTypes(Set.of(MediaType.ALL));
        var entryPoint = DelegatingAuthenticationEntryPoint.builder()
                .addEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), jsonRequests)
                .defaultEntryPoint(new LoginUrlAuthenticationEntryPoint(SecurityConstant.LOGIN_PATH))
                .build();

        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers(SecurityConstant.PUBLIC_PATHS).permitAll()
                        .requestMatchers(SecurityConstant.DOC_PATHS).permitAll()
                        .requestMatchers("/actuator/health/liveness", "/actuator/health/liveness/**").permitAll()
                        .requestMatchers("/admin", "/admin/**").hasRole(SecurityConstant.ADMIN)
                        .requestMatchers("/actuator/**").hasRole(SecurityConstant.ADMIN)
                        .anyRequest().authenticated())
                .oauth2Login(login -> login
                        .loginPage(SecurityConstant.LOGIN_PATH)
                        .failureHandler(new LoggingAuthenticationFailureHandler())
                        .authorizationEndpoint(endpoint -> endpoint.authorizationRequestResolver(resolver)))
                .logout(logout -> logout.logoutSuccessHandler(new CognitoLogoutSuccessHandler(properties)))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint));
        return http.build();
    }

    @Bean
    ClientRegistrationRepository clientRegistrationRepository(CognitoProperties properties) {
        return new CognitoClientRegistrationRepository(properties);
    }

    /** Resolved lazily so the app starts while the issuer is still a placeholder. */
    @Bean
    JwtDecoder jwtDecoder(CognitoProperties properties) {
        return new SupplierJwtDecoder(() -> {
            var decoder = NimbusJwtDecoder.withIssuerLocation(properties.issuerUri()).build();
            decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                    JwtValidators.createDefaultWithIssuer(properties.issuerUri()),
                    new AccessTokenClientValidator(properties.apiClientIds())));
            return decoder;
        });
    }

    /** Adds {@code ROLE_<group>} authorities from the ID token's Cognito groups at web sign-in. */
    @Bean
    GrantedAuthoritiesMapper cognitoAuthoritiesMapper() {
        return authorities -> {
            Set<GrantedAuthority> mapped = new HashSet<>(authorities);
            authorities.stream()
                    .filter(OidcUserAuthority.class::isInstance)
                    .map(OidcUserAuthority.class::cast)
                    .forEach(authority -> mapped.addAll(CognitoClaims.authorities(authority.getIdToken().getClaims())));
            return mapped;
        };
    }
}
