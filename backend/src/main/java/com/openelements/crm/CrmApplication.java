package com.openelements.crm;

import com.openelements.spring.base.mcp.McpConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main entry point for the Open CRM backend application.
 *
 * <p>As of spring-services 1.3.0 the library is a Spring Boot starter again:
 * {@code spring-services-core} registers {@code SpringServicesCoreAutoConfiguration}, which
 * imports the library's feature configurations and additively registers its entity and
 * repository packages under {@code com.openelements.spring.base} onto Boot's default scan. No
 * {@code @EntityScan}, {@code @EnableJpaRepositories} or {@code @Import(FullSpringServiceConfig)}
 * is therefore needed — an explicit {@code @EntityScan} would in fact suppress the additive
 * default scan. All CRM entities and repositories live under {@code com.openelements.crm}, so
 * Boot's default component scan of this package's tree covers them.
 *
 * <p>{@code @Import(McpConfiguration.class)} stays: that class is CRM's own MCP wiring under
 * {@code com.openelements.spring.base.mcp} (see spec 108/109), not the library's, and nothing
 * auto-configures it.
 */
@SpringBootApplication
@EnableAsync
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
@Import(McpConfiguration.class)
public class CrmApplication {

    public static void main(final String[] args) {
        SpringApplication.run(CrmApplication.class, args);
    }
}
