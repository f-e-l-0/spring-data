package com.example.inventory.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        // Return a mock user for demonstration/testing purposes.
        // In a real application, this would integrate with Spring Security:
        // SecurityContextHolder.getContext().getAuthentication().getName()
        return () -> Optional.of("system-user");
    }
}
