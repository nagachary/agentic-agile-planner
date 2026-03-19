package com.naga.ai.orchestrator.client;

import io.a2a.spec.Message;
import io.a2a.spec.TextPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class A2AMessageBuilder {
    private static final Logger logger = LoggerFactory.getLogger(A2AMessageBuilder.class);

    public Message buildMessage(String messageText) {
        logger.info("buildMessage :");
        return new Message.Builder()
                .role(Message.Role.USER)
                .messageId(UUID.randomUUID().toString())
                .parts(List.of(new TextPart(messageText)))
                .build();
    }

    public Message buildFollowUp(String messageText, String contextId) {
        logger.info("buildFollowUp :");
        return new Message.Builder()
                .role(Message.Role.USER)
                .messageId(UUID.randomUUID().toString())
                .contextId(contextId)
                .parts(List.of(new TextPart(messageText)))
                .build();
    }

    public Message buildApproval(String contextId, String projectKey) {
        logger.info("buildApproval :");
        String approvalText = "Approved projectKey:" + projectKey;
        return buildFollowUp(approvalText, contextId);
    }

}
