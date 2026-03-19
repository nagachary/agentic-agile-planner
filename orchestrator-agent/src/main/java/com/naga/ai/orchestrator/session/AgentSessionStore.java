package com.naga.ai.orchestrator.session;

import io.a2a.spec.AgentCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AgentSessionStore {

    private static final Logger logger = LoggerFactory.getLogger(AgentSessionStore.class);

    private final Map<String, AgentCard> agentSessionMap = new ConcurrentHashMap<>();

    public void save(String contextId, AgentCard agentCard) {
        logger.info("session saved for agent - {} with context id - {}", agentCard.name(), contextId);
        agentSessionMap.put(contextId, agentCard);
    }

    public AgentCard find(String contextId) {
        AgentCard card = agentSessionMap.get(contextId);
        if (null == card) {
            logger.warn("session not found for context id - {}", contextId);
        }

        return card;
    }

    public boolean exists(String contextId) {
        return agentSessionMap.containsKey(contextId);
    }

    public void remove(String contextId) {
        agentSessionMap.remove(contextId);
        logger.info("Session removed — contextId: {}", contextId);
    }
}
