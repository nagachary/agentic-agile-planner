package com.naga.ai.orchestrator.service;

import com.naga.ai.orchestrator.client.A2AAgentClient;
import com.naga.ai.orchestrator.client.AgentResponse;
import com.naga.ai.orchestrator.registry.AgentRegistry;
import com.naga.ai.orchestrator.session.AgentSessionStore;
import io.a2a.spec.AgentCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static com.naga.ai.orchestrator.constants.Constants.ANALYZE_REQUIREMENT_SKILL;

@Service
public class OrchestratorService {
    private static final Logger logger = LoggerFactory.getLogger(OrchestratorService.class);

    private final AgentRegistry agentRegistry;
    private final AgentSessionStore sessionStore;
    private final A2AAgentClient agentClient;

    public OrchestratorService(AgentRegistry agentRegistry, AgentSessionStore sessionStore, A2AAgentClient agentClient) {
        this.agentRegistry = agentRegistry;
        this.sessionStore = sessionStore;
        this.agentClient = agentClient;
    }

    public OrchestratorResponse startRequirement(String requirement) {
        logger.info("startRequirement : ");

        AgentCard agentCard = agentRegistry.findAgentBySkill(ANALYZE_REQUIREMENT_SKILL);
        if (null == agentCard) {
            throw new RuntimeException("No agent found for skill: "+ ANALYZE_REQUIREMENT_SKILL);
        }

        AgentResponse response = agentClient.startNewAgentSession(agentCard, requirement);
        sessionStore.save(response.contextId(), agentCard);
        logger.info("Session started — contextId: {}",response.contextId());

        return new OrchestratorResponse(response.contextId(), response.text());
    }
    public OrchestratorResponse refineRequirement(String contextId, String feedback) {
        logger.info("refineRequirement contextId - {}, feedback - {}", contextId, feedback);

        AgentCard agentCard = findAgentForSession(contextId);
        AgentResponse response = agentClient.continueSession(agentCard, feedback, contextId);
        logger.info("Refinement complete — contextId: {}", contextId);

        return new OrchestratorResponse(response.contextId(), response.text());
    }

    public OrchestratorResponse approveRequirement(String contextId,  String projectKey) {
        logger.info("Approving requirement — contextId: {} projectKey: {}",contextId, projectKey);

        AgentCard agentCard = findAgentForSession(contextId);
        AgentResponse response = agentClient.approve(agentCard, contextId, projectKey);
        sessionStore.remove(contextId);

        logger.info("Approval complete — contextId: {}", contextId);
        return new OrchestratorResponse(response.contextId(), response.text());
    }

    private AgentCard findAgentForSession(String contextId) {
        if (!sessionStore.exists(contextId)) {
            throw new RuntimeException("Session not found — contextId: " + contextId);
        }
        AgentCard agentCard = sessionStore.find(contextId);
        if (agentCard == null) {
            throw new RuntimeException("Agent not found for contextId: "+ contextId);
        }
        return agentCard;
    }

}
