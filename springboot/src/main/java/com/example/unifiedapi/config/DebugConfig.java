package com.example.unifiedapi.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

@Component
public class DebugConfig implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(DebugConfig.class);
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @Override
    public void run(String... args) throws Exception {
        logger.info("=== DEBUG: Checking loaded controllers ===");
        
        String[] controllerBeans = applicationContext.getBeanNamesForAnnotation(RestController.class);
        
        logger.info("Found {} @RestController beans:", controllerBeans.length);
        for (String beanName : controllerBeans) {
            Object bean = applicationContext.getBean(beanName);
            logger.info("  - {} ({})", beanName, bean.getClass().getName());
        }
        
        // Check if AuthController specifically is loaded
        try {
            Object authController = applicationContext.getBean("authController");
            logger.info("✅ AuthController found: {}", authController.getClass().getName());
        } catch (Exception e) {
            logger.error("❌ AuthController NOT found: {}", e.getMessage());
        }
        
        logger.info("=== END DEBUG ===");
    }
}
