package com.synergyhub;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableJpaRepositories
@EnableTransactionManagement
public class SynergyHubApplication {
    public static void main(String[] args) {
        // Load .env file if it exists
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();
        
        // Set environment variables from .env
        dotenv.entries().forEach(entry -> 
                System.setProperty(entry.getKey(), entry.getValue())
        );
        
        SpringApplication.run(SynergyHubApplication.class, args);
    }
}