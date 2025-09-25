package com.example.unifiedapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@ComponentScan(basePackages = {"com.example.unifiedapi"})
public class UnifiedOperationsApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(UnifiedOperationsApiApplication.class, args);
    }
}
