package com.naga.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CommonUtilityApplication {
   private static final Logger logger = LoggerFactory.getLogger(CommonUtilityApplication.class);

    public static void main(String[] args) {
        logger.info("CommonUtilityApplication");
        SpringApplication.run(CommonUtilityApplication.class, args);
    }
}
