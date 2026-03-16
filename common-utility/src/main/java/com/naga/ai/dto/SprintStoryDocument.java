package com.naga.ai.dto;

import java.util.HashMap;
import java.util.Map;

public class SprintStoryDocument {

    private final String content;

    // Metadata — stored as plain fields alongside the vector
    private final String ticketKey;
    private final String projectKey;
    private final String issueType;
    private final int storyPoints;
    private final String status;

    public SprintStoryDocument(String content, String ticketKey, String projectKey, String issueType, int storyPoints, String status) {
        this.content = content;
        this.ticketKey = ticketKey;
        this.projectKey = projectKey;
        this.issueType = issueType;
        this.storyPoints = storyPoints;
        this.status = status;
    }

    public String getContent() {
        return content;
    }

    public String getTicketKey() {
        return ticketKey;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public String getIssueType() {
        return issueType;
    }

    public int getStoryPoints() {
        return storyPoints;
    }

    public String getStatus() {
        return status;
    }

    /**
     * Converts SprintStoryDocument to a metadata Map.
     */
    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("ticketKey", ticketKey);
        metadata.put("projectKey", projectKey);
        metadata.put("issueType", issueType);
        metadata.put("storyPoints", storyPoints);
        metadata.put("status", status);
        return metadata;
    }

    @Override
    public String toString() {
        return "SprintStoryDocument { " +
                "content='" + content + '\'' +
                ", ticketKey='" + ticketKey + '\'' +
                ", projectKey='" + projectKey + '\'' +
                ", issueType='" + issueType + '\'' +
                ", storyPoints=" + storyPoints +
                ", status='" + status + '\'' +
                '}';
    }
}
