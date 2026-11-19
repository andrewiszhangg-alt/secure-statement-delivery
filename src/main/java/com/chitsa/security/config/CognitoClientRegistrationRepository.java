package com.chitsa.security.config;

import com.chitsa.security.util.SecurityConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;

/**
 * Builds the Cognito registration from OIDC discovery on first use, so the app still starts
 * while the properties are placeholders. Failures are not cached.
 */
@Slf4j
public class CognitoClientRegistrationRepository implements ClientRegistrationRepository {

    private final CognitoProperties properties;
    private volatile ClientRegistration registration;

    public CognitoClientRegistrationRepository(CognitoProperties properties) {
        this.properties = properties;
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        if (!SecurityConstant.REGISTRATION_ID.equals(registrationId)) {
            return null;
        }
        var current = registration;
        if (current == null) {
            synchronized (this) {
                if (registration == null) {
                    registration = build();
                }
                current = registration;
            }
        }
        return current;
    }

    private ClientRegistration build() {
        try {
            return ClientRegistrations.fromIssuerLocation(properties.issuerUri())
                    .registrationId(SecurityConstant.REGISTRATION_ID)
                    .clientId(properties.webClientId())
                    .clientSecret(properties.webClientSecret())
                    .scope(properties.scopes())
                    .redirectUri(SecurityConstant.REDIRECT_URI)
                    .build();
        } catch (RuntimeException ex) {
            log.error("Cognito discovery failed for issuerUri={}: {}", properties.issuerUri(), ex.getMessage());
            throw new IllegalStateException(SecurityConstant.COGNITO_NOT_CONFIGURED, ex);
        }
    }
}
