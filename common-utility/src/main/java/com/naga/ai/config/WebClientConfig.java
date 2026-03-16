package com.naga.ai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;

@Configuration
public class WebClientConfig {
    private static final Logger logger = LoggerFactory.getLogger(WebClientConfig.class);
    private final JiraProperties jiraProperties;

    public WebClientConfig(JiraProperties jiraProperties) {
        this.jiraProperties = jiraProperties;
    }

    private String basicAuthHeader() {
        String credentials = jiraProperties.getEmail()
                + ":" + jiraProperties.getApiToken();
        return Base64.getEncoder()
                .encodeToString(credentials.getBytes());
    }

    @Bean("JIRA_WEBCLIENT")
    public WebClient webClient() {
        logger.info("Initialising Jira WebClient for: {}", jiraProperties.getBaseUrl());
        return WebClient.builder()
                .defaultHeaders(header -> {
                    header.add(HttpHeaders.CONTENT_TYPE, "application/json");
                    header.add(HttpHeaders.ACCEPT, "application/json");
                    header.add("Authorization", "Basic " + basicAuthHeader());
                }).baseUrl(jiraProperties.getBaseUrl()).build();
    }

}
