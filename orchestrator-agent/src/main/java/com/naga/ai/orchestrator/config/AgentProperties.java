package com.naga.ai.orchestrator.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@ConfigurationProperties(prefix = "agents")
public class AgentProperties {
    private static final Logger logger = LoggerFactory.getLogger(AgentProperties.class);

    private final List<String> agentUrls;

    public AgentProperties(List<String> urls) {
        this.agentUrls = urls;
    }

    public List<String> getAgentUrls() {
        return agentUrls;
    }

    @PostConstruct
    public void validate() {
        logger.info("=== Agent Configuration ===");
        if (agentUrls == null || agentUrls.isEmpty()) {
            logger.warn("No agent URLs configured. " +
                    "Check agents.urls in " +
                    "application.properties");
        } else {
            logger.info("Configured agents: {}",
                    agentUrls.size());
            agentUrls.forEach(url ->
                    logger.info("  Agent URL: {}", url));
        }
    }
}
