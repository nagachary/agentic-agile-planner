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
    @Tool(description = """
            USE THIS TOOL when the user provides a NEW requirement
            or feature request for the first time.
            DO NOT use this for refinement or feedback on
            existing stories — just respond directly for those.
            This tool structures the requirement for LLM analysis.
            Input: the complete plain text requirement from user.
            Output: structured requirement ready for story generation.
            """)
    public String analyzeRequirement(
            @ToolParam(description =
                    "Complete plain text requirement exactly " +
                            "as provided by the user. Do not modify it.")
            String requirementText) {

        logger.info("Tool analyzeRequirement called " +
                "with requirement: {}", requirementText);

        // Returns structured context for LLM to generate stories
        return String.format("""
                Requirement received for analysis:
                %s
                
                Please generate user stories with Gherkin
                acceptance criteria for this requirement.
                Present them clearly numbered for user review.
                """, requirementText);
    }

    @Tool(description = """
            USE THIS TOOL FIRST when user approves the stories.
            Creates Epic in Jira BEFORE storing in VectorStore
            so the Epic key is available for metadata linking.
            Returns Epic key to use in next tool call.
            ALWAYS call storeAcceptanceCriteria AFTER this tool.
            """)
    public String createEpicInJira(
            @ToolParam(description =
                    "Short descriptive name for the Epic")
            String epicName,
            @ToolParam(description =
                    "Full requirement description")
            String requirementDescription) {

        logger.info("Tool createEpicInJira called: {}", epicName);

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
            return "Failed to create Epic: " + e.getMessage();
        }
    }

    @Tool(description = """
            USE THIS TOOL SECOND after createEpicInJira succeeds.
            Stores approved acceptance criteria in VectorStore
            WITH the Epic key from the previous tool call.
            Epic key is required — always call createEpicInJira first.
            """)
    public String storeAcceptanceCriteria(
            @ToolParam(description =
                    "Complete approved user stories and " +
                            "acceptance criteria text")
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
