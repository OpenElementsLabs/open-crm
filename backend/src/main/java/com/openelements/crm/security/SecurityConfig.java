package com.openelements.crm.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Enables method-level security so that {@code @PreAuthorize} annotations on controllers
 * are honoured. The actual {@code SecurityFilterChain} and JWT → authority mapping are
 * provided by {@code com.openelements.spring.base.security.SecurityConfig} from
 * {@code spring-services}, which maps the JWT {@code roles} claim to
 * {@code ROLE_<role>} authorities.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
}
