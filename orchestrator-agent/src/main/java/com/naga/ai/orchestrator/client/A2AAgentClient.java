package com.naga.ai.orchestrator.client;

import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.spec.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
public class A2AAgentClient {
    private static final Logger logger =  LoggerFactory.getLogger(A2AAgentClient.class);

    private final A2AMessageBuilder messageBuilder;

    public A2AAgentClient(A2AMessageBuilder messageBuilder) {
        this.messageBuilder = messageBuilder;
    }

    private AgentResponse send( AgentCard agentCard, Message message) {
        logger.info("send — agent: {} messageId: {}",agentCard.name(),message.messageId());

        try {
            JSONRPCTransport transport = new JSONRPCTransport(agentCard);
            logger.info("transport : {} ",transport);
            MessageSendParams params = MessageSendParams.builder().message(message).build();
            logger.info("params : {} ", params);
            EventKind eventKind = transport.sendMessage(params, null);
            logger.info("eventKind  {} ",eventKind);
            logger.info("EventKind received: {}",eventKind.getClass().getSimpleName());

            return buildResponse(eventKind);

        } catch (Exception e) {
            logger.error("Failed — agent: {} error: {}",agentCard.name(),e.getMessage());

            throw new RuntimeException("Agent communication failed: "+ e.getMessage(), e);
        }
    }


    private AgentResponse buildResponse(EventKind eventKind) {
        logger.info("buildResponse : {}", eventKind);

        if (eventKind instanceof Task task) {
            String contextId = task.contextId();
            String text = textFromTask(task);
            logger.info("Task — contextId: {} textLength: {}",contextId, text.length());

            return new AgentResponse(contextId, text);
        }

        if (eventKind instanceof Message msg) {
            String contextId = msg.contextId();
            String text = textFromMessage(msg);
            logger.info("Message — contextId: {} textLength: {}",contextId, text.length());

            return new AgentResponse(contextId, text);
        }

        logger.error("Unexpected EventKind type: {}", eventKind.getClass().getName());
        throw new RuntimeException("Unexpected EventKind: "+ eventKind.getClass().getSimpleName());
    }


    public AgentResponse startNewAgentSession(AgentCard agentCard, String messageText) {
        logger.info("startNewAgentSession — agent: {}", agentCard.name());

        Message message = messageBuilder.buildNewMessage(messageText);
        return send(agentCard, message);
    }

    public AgentResponse continueSession(AgentCard agentCard, String messageText, String contextId) {
        logger.info("continueSession — agent: {} contextId: {}",agentCard.name(), contextId);

        Message message = messageBuilder.buildFollowUpMessage( messageText, contextId);
        return send(agentCard, message);
    }

    public AgentResponse approve(AgentCard agentCard,String contextId,String projectKey) {
        logger.info("approve — agent: {} contextId: {} projectKey: {}",agentCard.name(),contextId, projectKey);

        Message message = messageBuilder.buildApprovalMessage(contextId, projectKey);
        return send(agentCard, message);
    }

    private String textFromTask(Task task) {
        logger.info("textFromTask : ");
        if (task.artifacts() == null || task.artifacts().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Artifact artifact : task.artifacts()) {
            for (Part<?> part : artifact.parts()) {
                if (part instanceof TextPart tp) {
                    if (tp.text() != null  && !tp.text().isBlank()) {
                        if (!sb.isEmpty()) {
                            sb.append("\n");
                        }
                        sb.append(tp.text());
                    }
                }
            }
        }

        return sb.toString();
    }

    private String textFromMessage(Message msg) {
        logger.info("textFromMessage : ");
        if (msg.parts() == null || msg.parts().isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (Part<?> part : msg.parts()) {
            if (part instanceof TextPart tp) {
                if (tp.text() != null  && !tp.text().isBlank()) {
                    if (!sb.isEmpty()) {
                        sb.append("\n");
                    }
                    sb.append(tp.text());
                }
            }
        }
        return sb.toString();
    }
}