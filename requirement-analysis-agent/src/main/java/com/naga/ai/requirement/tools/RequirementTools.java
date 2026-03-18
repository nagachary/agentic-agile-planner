package com.naga.ai.requirement.tools;

import com.naga.ai.client.JiraClient;
import com.naga.ai.dto.JiraTicket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Tools available to the LLM for requirement analysis workflow.
 * STRICT EXECUTION ORDER:
 * 1. analyzeRequirement  — when new requirement received
 * 2. createEpicInJira    — when user approves (FIRST on approval)
 * 3. storeAcceptanceCriteria — AFTER Epic created (needs epicKey)
 * Refinement is handled directly by LLM via ChatMemory — no tool.
 */
@Component
public class RequirementTools {
    private static final Logger logger = LoggerFactory.getLogger(RequirementTools.class);

    private final VectorStore vectorStore;
    private final JiraClient jiraClient;

    public RequirementTools(
            VectorStore vectorStore,
            JiraClient jiraClient) {
        this.vectorStore = vectorStore;
        this.jiraClient = jiraClient;
    }

    /**
     * Tool 1 — Analyze Requirement.
     * LLM calls this when user provides a new requirement.
     */
    @Tool(name = "analyzeRequirement", description = """
                  USE THIS TOOL ONLY ONCE when user provides
                               a NEW requirement for the first time.
                               DO NOT call this tool again after it returns.
                               DO NOT call this tool for refinement.
                               After this tool returns — generate stories
                               directly in your response without calling
                               any tool again.
            Input: the complete plain text requirement from user.
            Output: structured requirement ready for story generation.
            """)
    public String analyzeRequirement(
            @ToolParam(description =
                    "Complete plain text requirement exactly as provided by the user. Do not modify it.")
            String requirementText) {

        logger.info("Tool analyzeRequirement called " +
                "with requirement: {}", requirementText);

        // Returns structured context for LLM to generate stories
        return String.format("""
                Requirement received for analysis:
                %s
                """, requirementText);
    }

    @Tool(name = "createEpicInJira", description = """
            WHEN TO USE:
                            Call this tool FIRST on user approval only.
                            Never call before user explicitly approves.
                            Never call more than once per approval.
            
                            EXECUTION ORDER:
                            This tool must be called before storeAcceptanceCriteria.
                            Never call both tools at the same time.
                            Always wait for this tool result before calling storeAcceptanceCriteria.
            
                            PARAMETER RULES:
                            epicName must not be null or empty.
                            If epicName is null use requirement title from conversation memory.
                            requirementDescription must not be null.
                            If null use epicName value as fallback.
            
                            CALL LIMIT:
                            Call this tool EXACTLY ONCE per approval.
                            Never call more than once per session.
                            Never call in parallel with other tools.
            
                            RETURNS:
                            Real Epic key
                            Use this key in storeAcceptanceCriteria.
                            Never invent or assume the Epic key value.
            """)
    public String createEpicInJira(
            @ToolParam(description =
                    "Short descriptive name for the Epic")
            String epicName,
            @ToolParam(description =
                    "Full requirement description")
            String requirementDescription) {

        logger.info("Tool createEpicInJira called: {}", epicName);

        if (epicName == null || epicName.isBlank()) {
            logger.warn("createEpicInJira called with " +
                    "null epicName — skipping duplicate call");
            return "EpicName is NULL Or Empty";
        }

        try {
            JiraTicket epic = jiraClient.createTicket(
                    epicName,
                    requirementDescription,
                    "Epic"
            );

            logger.info("Epic created: {}", epic.getTicketKey());

            return "Epic created successfully. " +
                    "Epic key: " + epic.getTicketKey() +
                    ". Now call storeAcceptanceCriteria with " +
                    "this Epic key: " + epic.getTicketKey();

        } catch (Exception e) {
            logger.error("Failed to create Epic", e);
            return "EPIC_CREATION_FAILED : Failed to create Epic: " + e.getMessage();
        }
    }

    @Tool(name = "storeAcceptanceCriteria", description = """
            WHEN TO USE:
                            Call this tool SECOND on user approval only.
                            Never call before createEpicInJira succeeds.
                            Never call if createEpicInJira failed or returned EPIC_CREATION_FAILED.
            
                            EXECUTION ORDER:
                            Step 1 — createEpicInJira must be called first
                            Step 2 — this tool called second
                            Never call both tools at the same time.
                            Always wait for createEpicInJira result before calling this tool.
            
                            EPIC KEY RULES:
                            epicKey must be the real key returned by createEpicInJira.
                            Never invent or assume the epicKey value.
                            Never call this tool if epicKey is null.
            
                            CALL LIMIT:
                            Call this tool EXACTLY ONCE per approval.
                            Never call more than once per session.
            """)
    public String storeAcceptanceCriteria(
            @ToolParam(description =
                    "Complete approved user stories and acceptance criteria text")
            String approvedStories,
            @ToolParam(description =
                    "Session ID for this requirement session")
            String sessionId,
            @ToolParam(description =
                    "Epic key returned from createEpicInJira. " +
                            ".Required for metadata linking.")
            String epicKey,
            @ToolParam(description =
                    "Project key")
            String projectKey) {

        logger.info("Tool storeAcceptanceCriteria called " +
                "for Epic: {} session: {}", epicKey, sessionId);

        if (epicKey == null || epicKey.isBlank()) {
            logger.warn("epicKey is null");
            return "STORE_SKIPPED: epicKey is required and null.";
        }

        if (approvedStories == null
                || approvedStories.isBlank()) {
            logger.warn("approvedStories is null");
            return "STORE_SKIPPED: approvedStories is required and it cannot be null or empty.";
        }

        try {
            Document document = new Document(
                    approvedStories,
                    Map.of(
                            "source", "requirement-analysis-agent",
                            "type", "acceptance-criteria",
                            "epicKey", epicKey,
                            "projectKey", projectKey,
                            "sessionId", sessionId,
                            "status", "approved"
                    )
            );

            vectorStore.add(List.of(document));

            logger.info("Acceptance criteria stored " +
                    "for Epic: {}", epicKey);

            return "Acceptance criteria stored successfully. " +
                    "Epic key: " + epicKey +
                    ". Sprint Planner can now use this Epic.";

        } catch (Exception e) {
            logger.error("Failed to store acceptance criteria", e);
            return "Failed to store: " + e.getMessage();
        }
    }
}
