package com.openelements.crm;

import com.openelements.spring.base.FullSpringServiceConfig;
import com.openelements.spring.base.mcp.McpConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main entry point for the Open CRM backend application.
 *
 * <p>As of spring-services 1.1.0 the library's feature configurations are plain
 * {@code @Configuration} classes (no longer Spring Boot auto-configurations), so their
 * JPA entities and repositories under {@code com.openelements.spring.base} are no longer
 * registered automatically. {@link EntityScan} and {@link EnableJpaRepositories} therefore
 * scan the common {@code com.openelements} root so both the CRM's own and the library's
 * entities/repositories share one persistence unit.
 */
@SpringBootApplication
@EnableAsync
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
@EntityScan(basePackages = "com.openelements")
@EnableJpaRepositories(basePackages = "com.openelements")
@Import({FullSpringServiceConfig.class, McpConfiguration.class})
public class CrmApplication {

    public static void main(final String[] args) {
        SpringApplication.run(CrmApplication.class, args);
    }
}
