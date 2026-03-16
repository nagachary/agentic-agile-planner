package com.naga.ai.requirement.config;

import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AgentCardConfig {
    private final static Logger log = LoggerFactory.getLogger(AgentCardConfig.class);

    @Value("${server.port:8081}")
    private int port;

    @Value("${server.servlet.context-path:/requirement-analysis-agent}")
    private String contextPath;

    public AgentCard agentCard() {

        return new AgentCard.Builder()
                .capabilities(new AgentCapabilities.Builder().streaming(false).build())
                .description("""
                        	Requirement Analysis A2A agent that analyses plain text requirements,
                        		generates user stories with acceptance criteria,
                        		supports iterative refinement with human in loop approval before it
                        		creates an Epic in Jira and stores acceptance
                        		criteria in VectorStore for downstream agents.
                        """)
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .documentationUrl("")
                .name("Requirement Analysis Tool")
                .protocolVersion("1.0.0")
                .skills(List.of(
                        new AgentSkill.Builder()
                                .id("analyze-requirement")
                                .name("Analyze Requirement")
                                .description(
                                        "Accepts plain text requirement " +
                                                "and generates user stories " +
                                                "with acceptance criteria using " +
                                                "Ollama mistral LLM")
                                .tags(List.of(
                                        "requirement",
                                        "analysis",
                                        "user-stories",
                                        "acceptance-criteria"))
                                .examples(List.of(
                                        "Build JWT authentication " +
                                                "with login and token generation"))
                                .build(),
                        new AgentSkill.Builder()
                                .id("refine-stories")
                                .name("Refine Stories")
                                .description(
                                        "Refines previously generated " +
                                                "user stories based on human " +
                                                "feedback using conversation " +
                                                "history for context")
                                .tags(List.of(
                                        "refinement",
                                        "feedback",
                                        "human-in-loop"))
                                .examples(List.of(
                                        "Add refresh token to story 1",
                                        "Split story 2 into two stories"))
                                .build(),
                        new AgentSkill.Builder()
                                .id("approve-and-store")
                                .name("Approve and Store")
                                .description(
                                        "Stores approved acceptance " +
                                                "criteria in VectorStore and " +
                                                "creates Epic in Jira for " +
                                                "downstream agents")
                                .tags(List.of(
                                        "approval",
                                        "jira",
                                        "vector-store",
                                        "epic"))
                                .examples(List.of(
                                        "Approved",
                                        "Accept and create Epic"))
                                .build()
                ))
                .supportsAuthenticatedExtendedCard(false)
                .url("http://localhost:" + port + contextPath)
                .build();

    }
}
