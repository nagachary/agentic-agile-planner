package com.naga.ai.orchestrator.client;

import io.a2a.client.Client;
import io.a2a.client.config.ClientConfig;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class A2AAgentClient {
    private static final Logger logger = LoggerFactory.getLogger(A2AAgentClient.class);
    private static final int TIMEOUT_SECONDS = 600;

    private final A2AMessageBuilder messageBuilder;
    private final A2AResponseParser responseParser;

    public A2AAgentClient(A2AMessageBuilder messageBuilder, A2AResponseParser responseParser) {
        this.messageBuilder = messageBuilder;
        this.responseParser = responseParser;
    }

    private AgentResponse send(AgentCard agentCard, Message message) {
        logger.info("Sending message — agent: {} messageId: {}", agentCard.name(), message.getMessageId());

        ClientConfig clientConfig = ClientConfig.builder().setAcceptedOutputModes(List.of("text")).build();
        Client client = Client.builder(agentCard).clientConfig(clientConfig).build();
        CompletableFuture<AgentResponse> future = new CompletableFuture<>();

        client.sendMessage(message,
                List.of((event, card) -> {
                    try {
                        String contextId = responseParser.extractContextId(event);
                        String text = responseParser.extractText(event);
                        logger.info("From sendMessage contextId - {}, text - {}", contextId, text);
                        future.complete(new AgentResponse(contextId, text));
                    } catch (Exception exp) {
                        logger.error("Error Parsing - event : {} ", exp.getMessage());
                        future.completeExceptionally(exp);
                    }
                }),
                error -> {
                    logger.error("Error while sending message : {}", agentCard.name(), error);
                    future.completeExceptionally(error);
                });

        try {

            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            logger.error("Timeout waiting for agent: {} after {}s", agentCard.name(), TIMEOUT_SECONDS);
            throw new RuntimeException("Agent timed out after " + TIMEOUT_SECONDS + "s: " + agentCard.name(), e);
        } catch (Exception exp) {
            logger.error("Agent communication failed — agent: {} error: {}", agentCard.name(), exp.getMessage());
            throw new RuntimeException("Agent communication failed: " + exp.getMessage(), exp);
        }
    }

    public AgentResponse startNewAgentSession(AgentCard agentCard, String messageText) {
        logger.info("Starting new session — agent: {}", agentCard.name());
        Message message = messageBuilder.buildMessage(messageText);
        return send(agentCard, message);
    }

    public AgentResponse continueSession(AgentCard agentCard, String messageText, String contextId) {
        logger.info("Continuing session — agent: {} contextId: {}", agentCard.name(), contextId);
        Message message = messageBuilder.buildFollowUp(messageText, contextId);
        return send(agentCard, message);
    }

    public AgentResponse approve(AgentCard agentCard, String contextId, String projectKey) {
        logger.info("Sending approval — agent: {} contextId: {} projectKey: {}", agentCard.name(), contextId, projectKey);
        Message message = messageBuilder.buildApproval(contextId, projectKey);
        return send(agentCard, message);
    }
}
