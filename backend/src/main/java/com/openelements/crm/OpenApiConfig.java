package com.openelements.crm;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList("oidc"))
                .components(new Components()
                        .addSecuritySchemes("oidc", new SecurityScheme()
                                .type(SecurityScheme.Type.OPENIDCONNECT)
                                .openIdConnectUrl(issuerUri + "/.well-known/openid-configuration")
                        )
                );
    }
}
