package com.naga.ai.orchestrator;

import com.naga.ai.orchestrator.config.AgentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AgentProperties.class)
public class OrchestratorApplication {
	private static final Logger logger = LoggerFactory.getLogger(OrchestratorApplication.class);
	public static void main(String[] args) {
		logger.info("OrchestratorApplication : ");
		SpringApplication.run(OrchestratorApplication.class, args);
	}

}
