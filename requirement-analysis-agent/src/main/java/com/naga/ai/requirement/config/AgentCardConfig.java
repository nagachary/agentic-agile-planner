package com.naga.ai.requirement.config;

import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.AgentSkill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AgentCardConfig {

    private static final Logger logger = LoggerFactory.getLogger(AgentCardConfig.class);

    @Value("${server.port:8081}")
    private int port;

    @Bean
    public AgentCard agentCard() {
        String agentUrl = "http://localhost:" + port;
        logger.info("Building AgentCard url: {}", agentUrl);

        return AgentCard.builder()
                .name("Requirement Analysis Tool")
                .description("""
                        Requirement Analysis A2A agent
                        that analyses plain text requirements,
                        generates user stories with acceptance
                        criteria, supports iterative refinement
                        with human in loop approval before it
                        creates an Epic in Jira and stores
                        acceptance criteria in VectorStore
                        for downstream agents.
                        """)
                .version("2.0.0")
                .documentationUrl("")
                .protocolVersion(AgentCard.CURRENT_PROTOCOL_VERSION)
                .capabilities(AgentCapabilities.builder()
                                .streaming(false)
                                .pushNotifications(false)
                                .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .supportedInterfaces(List.of(new AgentInterface("JSONRPC",agentUrl)))
                .supportsExtendedAgentCard(false)
                .skills(List.of(
                        AgentSkill.builder()
                                .id("analyze-requirement")
                                .name("Analyze Requirement")
                                .description(
                                        "Accepts plain text " +
                                                "requirement and generates " +
                                                "user stories with " +
                                                "acceptance criteria " +
                                                "using Ollama LLM")
                                .tags(List.of(
                                        "requirement",
                                        "analysis",
                                        "user-stories",
                                        "acceptance-criteria"))
                                .examples(List.of("Build JWT authentication with login and token generation"))
                                .build(),
                        AgentSkill.builder()
                                .id("refine-stories")
                                .name("Refine Stories")
                                .description("Refines previously " +
                                                "generated user stories " +
                                                "based on human feedback " +
                                                "using conversation " +
                                                "history for context")
                                .tags(List.of("refinement", "feedback","human-in-loop"))
                                .examples(List.of(
                                        "Add refresh token to story 1",
                                        "Split story 2 into two stories"))
                                .build(),
                        AgentSkill.builder()
                                .id("approve-and-store")
                                .name("Approve and Store")
                                .description("Stores approved acceptance " +
                                                "criteria in VectorStore " +
                                                "and creates Epic in Jira " +
                                                "for downstream agents")
                                .tags(List.of("approval", "jira", "vector-store", "epic"))
                                .examples(List.of("Approved","Accept and create Epic"))
                                .build()
                )).build();
    }
}