package com.naga.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

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
