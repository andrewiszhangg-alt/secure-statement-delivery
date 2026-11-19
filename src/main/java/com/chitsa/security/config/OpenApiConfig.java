package com.chitsa.security.config;

import com.chitsa.security.util.SecurityConstant;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import org.springframework.context.annotation.Configuration;

/**
 * Two ways to call the API from Scalar: an admin's Cognito access token (bearerAuth), or the signed-in
 * web session with the XSRF-TOKEN cookie value sent as X-XSRF-TOKEN (csrfToken).
 */
@Configuration
@OpenAPIDefinition(security = {
        @SecurityRequirement(name = SecurityConstant.BEARER_AUTH),
        @SecurityRequirement(name = SecurityConstant.CSRF_AUTH)
})
@SecuritySchemes({
        @SecurityScheme(name = SecurityConstant.BEARER_AUTH, type = SecuritySchemeType.HTTP,
                scheme = SecurityConstant.BEARER, bearerFormat = SecurityConstant.JWT),
        @SecurityScheme(name = SecurityConstant.CSRF_AUTH, type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.HEADER,
                paramName = SecurityConstant.CSRF_HEADER, description = SecurityConstant.CSRF_HEADER_DESCRIPTION)
})
public class OpenApiConfig {
}
