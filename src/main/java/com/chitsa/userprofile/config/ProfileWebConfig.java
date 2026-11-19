package com.chitsa.userprofile.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Applies the profile-completion gate to the home and accounts/statements pages. */
@Configuration
@RequiredArgsConstructor
public class ProfileWebConfig implements WebMvcConfigurer {

    private final ProfileCompletionInterceptor profileCompletionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(profileCompletionInterceptor).addPathPatterns("/", "/accounts/**", "/accounts");
    }
}
