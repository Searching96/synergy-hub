package com.synergyhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.synergyhub")
public class SynergyHubApplication {
    public static void main(String[] args) {
        SpringApplication.run(SynergyHubApplication.class, args);
    }
}