package com.naga.ai.orchestrator.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "agents")
public class AgentProperties {
    private static final Logger logger = LoggerFactory.getLogger(AgentProperties.class);

    private List<String> urls;

    @ConstructorBinding
    public AgentProperties(List<String> urls) {
        this.urls = urls;
    }

    public List<String> getUrls() {
        return urls;
    }

    public void setUrls(List<String> urls) {
        this.urls = urls;
    }

    @PostConstruct
    public void validate() {
        logger.info("=== Agent Configuration ===");
        if (urls == null || urls.isEmpty()) {
            logger.warn("No agent URLs configured. " +
                    "Check agents.urls in " +
                    "application.properties");
        } else {
            logger.info("Configured agents: {}",
                    urls.size());
            urls.forEach(url ->
                    logger.info("  Agent URL: {}", url));
        }
    }
}
