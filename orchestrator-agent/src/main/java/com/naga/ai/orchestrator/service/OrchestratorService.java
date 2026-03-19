package com.naga.ai.orchestrator.service;

import com.naga.ai.orchestrator.client.A2AAgentClient;
import com.naga.ai.orchestrator.client.AgentResponse;
import com.naga.ai.orchestrator.registry.AgentRegistry;
import com.naga.ai.orchestrator.session.AgentSessionStore;
import io.a2a.spec.AgentCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import static com.naga.ai.orchestrator.constants.Constants.ANALYZE_REQUIREMENT_SKILL;

@Service
public class OrchestratorService {
    private static final Logger logger = LoggerFactory.getLogger(OrchestratorService.class);

    private final AgentRegistry agentRegistry;
    private final AgentSessionStore sessionStore;
    private final A2AAgentClient agentClient;
    private final ChatClient chatClient;


    public OrchestratorService(AgentRegistry agentRegistry, AgentSessionStore sessionStore, A2AAgentClient agentClient, ChatClient chatClient) {
        this.agentRegistry = agentRegistry;
        this.sessionStore = sessionStore;
        this.agentClient = agentClient;
        this.chatClient = chatClient;
    }

    /**
     * Starts new requirement analysis.
     * LLM calls this first to generate stories.
     * Returns contextId + generated stories.
     */
    @Tool(description = """
            Sends a requirement to the Requirement
            Analysis Agent to generate user stories
            with acceptance criteria.
            Returns contextId and generated stories.
            Call this first with the user requirement.
            """)
    public String startRequirement(String requirement) {
        logger.info("Tool - startRequirement : ");

        AgentCard agentCard = agentRegistry.findAgentBySkill(ANALYZE_REQUIREMENT_SKILL);
        if (null == agentCard) {
            throw new RuntimeException("No agent found for skill: "+ ANALYZE_REQUIREMENT_SKILL);
        }

        AgentResponse response = agentClient.startNewAgentSession(agentCard, requirement);
        sessionStore.save(response.contextId(), agentCard);
        logger.info("Session started — contextId: {}",response.contextId());

       // return new OrchestratorResponse(response.contextId(), response.text());
        return "contextId:" + response.contextId() + "\n" + response.text();
    }

    /**
     * Sends refinement feedback to agent.
     * LLM calls this if stories need improvement.
     * Returns refined stories.
     */
    @Tool(description = """
            Sends refinement feedback to the
            Requirement Analysis Agent to improve
            the generated user stories.
            Use contextId from startRequirement.
            Call this if stories are incomplete
            or need more detail.
            """)
    public String refineRequirement(String contextId, String feedback) {
        logger.info("refineRequirement contextId - {}, feedback - {}", contextId, feedback);

        AgentCard agentCard = findAgentForSession(contextId);
        AgentResponse response = agentClient.continueSession(agentCard, feedback, contextId);
        logger.info("Refinement complete — contextId: {}", contextId);

        //return new OrchestratorResponse(response.contextId(), response.text());
        return "contextId:" + response.contextId() + "\n" + response.text();
    }

    /**
     * Approves stories and creates Epic in Jira.
     * LLM calls this when stories are satisfactory.
     * Removes session after approval.
     * Returns Epic creation confirmation.
     */
    @Tool(description = """
            Approves the generated user stories
            and creates an Epic in Jira.
            Use contextId from startRequirement.
            Use projectKey provided by the user.
            Call this when stories are complete
            and ready for sprint planning.
            """)
    public String approveRequirement(String contextId,  String projectKey) {
        logger.info("Approving requirement — contextId: {} projectKey: {}",contextId, projectKey);

        AgentCard agentCard = findAgentForSession(contextId);
        AgentResponse response = agentClient.approve(agentCard, contextId, projectKey);
        sessionStore.remove(contextId);

        logger.info("Approval complete — contextId: {}", contextId);
       // return new OrchestratorResponse(response.contextId(), response.text());
        return response.text();
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

    /**
     * AI-powered entry point.
     * LLM autonomously drives the full workflow:
     *   1. Analyzes requirement
     *   2. Refines if needed
     *   3. Approves when satisfied
     * Returns final result to user.
     */
    public String plan(String requirement, String projectKey) {
        logger.info("plan — requirement: {}, projectKey: {}",requirement, projectKey);

        return chatClient.prompt()
                .system("""
                        You are an agile sprint planning orchestrator.
                        Your job is to autonomously plan a sprint by:
                        
                        1. Call startRequirement with the requirement
                           and projectKey to generate user stories.
                        
                        2. Review the stories carefully.
                           If they are incomplete, unclear or missing
                           acceptance criteria — call refineRequirement
                           with specific feedback to improve them.
                        
                        3. When stories are complete and clear —
                           call approveRequirement to create the Epic
                           in Jira and store the acceptance criteria.
                        
                        4. Return a summary to the user including:
                           - The Epic key created
                           - Number of stories generated
                           - Brief description of what was planned
                        
                        Always complete the full workflow.
                        Never stop after just analyzing.
                        Always end with approveRequirement.
                        """)
                .user("Requirement: " + requirement
                        + "\nProjectKey: " + projectKey)
                .tools(this)
                .call()
                .content();
    }

}
