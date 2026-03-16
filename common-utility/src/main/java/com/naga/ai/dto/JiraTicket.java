package com.naga.ai.dto;

public class JiraTicket {

    private final String ticketKey;
    private final String summary;
    private final String description;
    private final String issueType;

    public JiraTicket(String ticketKey, String summary, String description, String issueType) {
        this.ticketKey = ticketKey;
        this.summary = summary;
        this.description = description;
        this.issueType = issueType;
    }

    public String getTicketKey() {
        return ticketKey;
    }

    public String getSummary() {
        return summary;
    }

    public String getDescription() {
        return description;
    }

    public String getIssueType() {
        return issueType;
    }

    @Override
    public String toString() {
        return "JiraTicket { " +
                "ticketKey='" + ticketKey + '\'' +
                ", summary='" + summary + '\'' +
                ", description='" + description + '\'' +
                ", issueType='" + issueType + '\'' +
                '}';
    }
}
