package com.naga.ai;

import com.naga.ai.config.JiraProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = {
        "com.naga.ai"
})
@EnableConfigurationProperties(JiraProperties.class)
public class RequirementAnalysisAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(RequirementAnalysisAgentApplication.class, args);
    }

}
