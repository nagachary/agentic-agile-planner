package com.naga.ai.requirement.service;

import com.naga.ai.requirement.controller.RequirementController;
import com.naga.ai.requirement.model.AnalysisResponse;
import com.naga.ai.requirement.tools.RequirementTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RequirementService {
    private static final Logger logger =
            LoggerFactory.getLogger(RequirementService.class);

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final RequirementTools requirementTools;

    public RequirementService(
            ChatClient chatClient,
            ChatMemory chatMemory,
            RequirementTools requirementTools) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
        this.requirementTools = requirementTools;
    }

    public AnalysisResponse analyzeRequirement(
            String requirementText) {

        String sessionId = UUID.randomUUID().toString();

        logger.info("Starting new requirement analysis " +
                "session: {}", sessionId);

        String response = callLlm(requirementText, sessionId);

        logger.info("Requirement analysis complete " +
                "for session: {}", sessionId);

        return new AnalysisResponse(sessionId, response);
    }

    public String refineStories(
            String feedback,
            String sessionId) {

        logger.info("Refining stories for session: {} " +
                "with feedback: {}", sessionId, feedback);

        String response = callLlm(feedback, sessionId);

        logger.info("Refinement complete for session: {}",
                sessionId);

        return response;
    }

    public String approveStories(
            String sessionId,
            String projectKey) {

        logger.info("Approving stories for session: {}",
                sessionId);

        String approvalMessage = String.format("""
                The user has approved the stories.
                Session ID for this approval: %s
                Project key: %s
                
                Please proceed in this exact order:
                1. Call createEpicInJira with a suitable
                   Epic name and requirement description
                2. Use the returned Epic key to call
                   storeAcceptanceCriteria
                3. Confirm completion to the user with
                   the Epic key
                """, sessionId, projectKey);
        String response = callLlm(approvalMessage, sessionId);
        clearSession(sessionId);

        logger.info("Approval complete for session: {}",
                sessionId);

        return response;
    }

    private String callLlm(
            String message,
            String sessionId) {

        return chatClient.prompt()
                .user(message)
                .tools(requirementTools)
                .advisors(advisor -> advisor
                        .param(
                                ChatMemory
                                        .CONVERSATION_ID,
                                sessionId
                        )
                )
                .call()
                .content();
    }


    public void clearSession(String sessionId) {
        logger.info("Clearing session memory: {}", sessionId);
        chatMemory.clear(sessionId);
    }
}
