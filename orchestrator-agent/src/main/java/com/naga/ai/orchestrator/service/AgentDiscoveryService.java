package com.naga.ai.orchestrator.service;

import com.naga.ai.orchestrator.config.AgentProperties;
import com.naga.ai.orchestrator.registry.AgentRegistry;
import io.a2a.client.http.A2ACardResolver;
import io.a2a.spec.AgentCard;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the agent discovery service for the registered agents using the agent cards
 */
@Service
public class AgentDiscoveryService {
    private static final Logger logger = LoggerFactory.getLogger(AgentDiscoveryService.class);

    private final AgentProperties agentProperties;
    private final AgentRegistry agentRegistry;

    public AgentDiscoveryService(AgentProperties agentProperties, AgentRegistry agentRegistry) {
        this.agentProperties = agentProperties;
        this.agentRegistry = agentRegistry;
    }

    @PostConstruct
    public void discoverAgents() {
        logger.info("discoverAgents : ");

        List<String> agentUrls = agentProperties.getAgentUrls();
        if (agentUrls == null || agentUrls.isEmpty()) {
            logger.warn("No agent URLs configured ");
            return;
        }

        for (String agentUrl : agentUrls) {
            discoverAgent(agentUrl);
        }

        logger.info("No of registered skills are : {}", agentRegistry.skillCount());
    }

    private void discoverAgent(String agentUrl) {
        logger.info("discoverAgent agent at: {}", agentUrl);

        try {
            AgentCard agentCard = new A2ACardResolver(agentUrl).getAgentCard();
            agentRegistry.register(agentCard);

            logger.info("Discovered Agent is {}, its skill are : {}", agentCard.name(), agentCard.skills() != null ? agentCard.skills() : 0);

        } catch (Exception e) {
            logger.error("Failed to discover agent at : {} => Error Message {}", agentUrl, e.getMessage());
        }
    }
}
