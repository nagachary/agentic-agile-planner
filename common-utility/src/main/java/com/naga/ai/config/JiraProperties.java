package com.naga.ai.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@ConfigurationProperties(prefix = "jira")
public class JiraProperties {
    private String baseUrl;
    private String email;
    private String apiToken;
    private String projectSpaceKey;
    private String storyPointsField;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public String getProjectSpaceKey() {
        return projectSpaceKey;
    }

    public void setProjectSpaceKey(String projectSpaceKey) {
        this.projectSpaceKey = projectSpaceKey;
    }

    public String getStoryPointsField() {
        return storyPointsField;
    }

    public void setStoryPointsField(String storyPointsField) {
        this.storyPointsField = storyPointsField;
    }

    @PostConstruct
    public void validate() {
        Objects.requireNonNull(baseUrl,
                "jira.baseUrl is required");
        Objects.requireNonNull(email,
                "jira.email is required — " +
                        "set JIRA_EMAIL env variable");
        Objects.requireNonNull(apiToken,
                "jira.apiToken is required — " +
                        "set JIRA_API_TOKEN env variable");
        Objects.requireNonNull(projectSpaceKey,
                "jira.projectSpaceKey is required — " +
                        "set JIRA_PROJECT_KEY env variable");
    }

    @Override
    public String toString() {
        return "JiraProperties { " +
                "baseUrl='" + baseUrl + '\'' +
                ", email='" + email + '\'' +
                ", apiToken='" + apiToken + '\'' +
                ", projectSpaceKey='" + projectSpaceKey + '\'' +
                ", storyPointsField='" + storyPointsField + '\'' +
                '}';
    }
}
