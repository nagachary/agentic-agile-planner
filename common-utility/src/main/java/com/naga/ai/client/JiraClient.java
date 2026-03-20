package com.naga.ai.client;

import com.naga.ai.config.JiraProperties;
import com.naga.ai.dto.JiraTicket;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared Jira REST API client used by AI agents.
 * Provides methods to create tickets, fetch sprint history and update story points.
 */
@Component
public class JiraClient {
    private static final Logger logger = LoggerFactory.getLogger(JiraClient.class);

    private final WebClient webClient;
    private final JiraProperties jiraProperties;

    public JiraClient(@Qualifier("JIRA_WEBCLIENT") WebClient webClient, JiraProperties jiraProperties) {
        this.webClient = webClient;
        this.jiraProperties = jiraProperties;
    }

    @PostConstruct
    public void testConnection() {
        logger.info("Verifying Jira connection to: {}", jiraProperties.getBaseUrl());
        try {
            webClient.get()
                    .uri("/rest/api/3/myself")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            logger.info("Jira connection verified successfully");
        } catch (WebClientResponseException e) {
            logger.error("Jira connection failed — status: {}, message: {}",e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("Jira connection failed :— {}",e.getMessage());
        }
    }


    /**
     * Creates a new Jira story ticket with the given summary, description and issue type.
     * Returns a JiraTicket DTO containing the generated ticket key
     */
    public JiraTicket createTicket(String summary,
                                   String description,
                                   String issueType) {
        logger.info("Creating Jira ticket — summary: {}", summary);

        try {
            Map<String, Object> body = buildTicketBody(summary, description, issueType);
            Map response = webClient.post().uri("/rest/api/3/issue")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            String ticket = (String) response.get("key");
            logger.info("Ticket created successfully — key: {}", ticket);

            return new JiraTicket( ticket,summary,description,issueType
            );
        } catch (WebClientResponseException e) {
            logger.error("Failed to create ticket :— status: {} body: {}", e.getStatusCode(),e.getResponseBodyAsString());
            throw new RuntimeException("Jira ticket creation failed: "+ e.getMessage());
        }

    }

    private Map<String, Object> buildTicketBody(String summary, String description, String issueType) {
        Map<String, Object> descContent = Map.of(
                "type", "doc",
                "version", 1,
                "content", List.of(
                        Map.of(
                                "type", "paragraph",
                                "content", List.of(
                                        Map.of(
                                                "type", "text",
                                                "text", description
                                        )
                                )
                        )
                )
        );

        Map<String, Object> fields = new HashMap<>();
        fields.put("project", Map.of("key", jiraProperties.getProjectSpaceKey()));
        fields.put("summary", summary);
        fields.put("description", descContent);
        fields.put("issuetype", Map.of("name", issueType));

        return Map.of("fields", fields);
    }

    /**
     * Fetches the last 50 completed stories from the configured Jira project using JQL.
     * Returns raw JSON string used by the Estimation agent for story point analysis.
     */
    public String getSprintHistory() {
        String projectSpaceKey = jiraProperties.getProjectSpaceKey();
        String storyPointField = jiraProperties.getStoryPointsField();
        logger.info("Fetching sprint history for project: {}", projectSpaceKey);

        String jql = "project=" + projectSpaceKey
                + " AND issuetype=Story"
                + " AND status=Done"
                + " ORDER BY updated DESC";
        try {
            String result = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/rest/api/3/search")
                            .queryParam("jql", jql)
                            .queryParam("fields",
                                    "summary,status,"
                                            + storyPointField)
                            .queryParam("maxResults", "50")
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            logger.info("Sprint history fetched successfully");
            return result;
        } catch (WebClientResponseException e) {
            logger.error("Failed to fetch sprint history :— {}", e.getMessage());
            throw new RuntimeException( "Sprint history fetch failed: "+ e.getMessage());
        }
    }

    /**
     * Updates the story points field on an existing Jira ticket identified by issue key.
     * Uses the configured custom field ID e.g. customfield_10016 to set the point value.
     */
    public void updateStoryPoints(String issueKey, int storyPoints) {
        logger.info("Updating story points — issue: {} points: {}", issueKey, storyPoints);

        try {
            webClient.put()
                    .uri("/rest/api/3/issue/" + issueKey)
                    .bodyValue(Map.of(
                            "fields", Map.of(
                                    jiraProperties
                                            .getStoryPointsField(),
                                    storyPoints
                            )
                    ))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
            logger.info("Story points updated :—  issue: {} points: {}", issueKey, storyPoints);
        } catch (WebClientResponseException exception) {
            logger.error("Failed to update story points :—  issue: {} error: {}", issueKey, exception.getMessage());
            throw new RuntimeException("Story points update failed: "+ exception.getMessage());
        }
    }

    /**
     * Creates a Story ticket linked to an Epic.
     * Uses parent field for Epic link in Jira cloud.
     */
    public JiraTicket createTicketWithEpicLink(
            String summary,
            String description,
            String issueType,
            String epicKey) {
        logger.info("Creating Jira ticket —  summary: {} epicKey: {}",summary, epicKey);
        try {
            Map<String, Object> body = buildTicketBodyWithEpic(summary, description,issueType, epicKey);
            Map response = webClient.post()
                    .uri("/rest/api/3/issue")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            String ticket = (String) response.get("key");
            logger.info("Ticket created — key: {}", ticket);

            return new JiraTicket(ticket, summary, description, issueType);

        } catch (WebClientResponseException e) {
            logger.error("Failed to create ticket — status: {} body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException( "Jira ticket creation failed: "+ e.getMessage());
        }
    }

    private Map<String, Object> buildTicketBodyWithEpic( String summary, String description, String issueType, String epicKey) {
        Map<String, Object> descContent = Map.of(
                "type", "doc",
                "version", 1,
                "content", List.of(
                        Map.of(
                                "type", "paragraph",
                                "content", List.of(
                                        Map.of(
                                                "type", "text",
                                                "text", description != null
                                                        ? description
                                                        : summary
                                        )
                                )
                        )
                )
        );

        Map<String, Object> fields = new HashMap<>();
        fields.put("project",Map.of("key", jiraProperties.getProjectSpaceKey()));
        fields.put("summary", summary);
        fields.put("description", descContent);
        fields.put("issuetype", Map.of("name", issueType));

        if (epicKey != null && !epicKey.isBlank()) {
            fields.put("parent", Map.of("key", epicKey));
        }

        return Map.of("fields", fields);
    }
}
