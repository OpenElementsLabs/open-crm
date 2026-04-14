package com.openelements.crm;

import com.openelements.spring.base.FullSpringServiceConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main entry point for the Open CRM backend application.
 */
@SpringBootApplication
@EnableAsync
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
@Import({FullSpringServiceConfig.class})
public class CrmApplication {

    public static void main(final String[] args) {
        SpringApplication.run(CrmApplication.class, args);
    }
}
