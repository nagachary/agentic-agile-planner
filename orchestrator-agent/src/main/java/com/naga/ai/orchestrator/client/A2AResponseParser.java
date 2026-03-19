package com.naga.ai.orchestrator.client;

import io.a2a.client.ClientEvent;
import io.a2a.client.MessageEvent;
import io.a2a.client.TaskEvent;
import io.a2a.client.TaskUpdateEvent;
import io.a2a.spec.Artifact;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class A2AResponseParser {
    private static final Logger logger = LoggerFactory.getLogger(A2AResponseParser.class);

    public String extractContextId(ClientEvent event) {
        logger.info("extractContextId :");
        if (null == event) {
            logger.warn("Client Event is null — cannot extract contextId");
            return null;
        }

        String contextId = null;
        if (event instanceof MessageEvent e) {
            contextId = e.getMessage().getContextId();
        } else if (event instanceof TaskEvent e) {
            contextId = e.getTask().getContextId();
        } else if (event instanceof TaskUpdateEvent e) {
            contextId = e.getTask().getContextId();
        } else {
            logger.warn("Unknown client event : {} ", event.getClass().getSimpleName());
        }

        if (null == contextId || contextId.isBlank()) {
            logger.warn("contextId is missing - from event response");
            return null;
        }

        logger.info("Extracted contextId: {}", contextId);

        return contextId;
    }

    public String extractText(ClientEvent event) {
        logger.info("extractText :");

        if (null == event) {
            logger.warn("event is null — cannot extract text from event");
            return null;
        }

        if (event instanceof MessageEvent e) {
            return textFromParts(e.getMessage().getParts());
        }
        if (event instanceof TaskEvent e) {
            return textFromArtifacts(e.getTask().getArtifacts());
        }
        if (event instanceof TaskUpdateEvent e) {
            return textFromArtifacts(e.getTask().getArtifacts());
        }

        logger.warn("Unknown ClientEvent type: {}", event.getClass().getSimpleName());

        return "";
    }

    private String textFromParts(List<? extends Part<?>> parts) {
        logger.info("textFromParts :");

        if (parts == null || parts.isEmpty()) {
            return "";
        }

        StringBuilder response = new StringBuilder();
        for (Part<?> part : parts) {
            if (part instanceof TextPart tp) {
                String text = tp.getText();
                if (text != null
                        && !text.isBlank()) {
                    if (!response.isEmpty()) {
                        response.append("\n");
                    }
                    response.append(text);
                }
            }
        }
        return response.toString();
    }

    private String textFromArtifacts(List<Artifact> artifacts) {
        logger.info("textFromArtifacts :");

        if (artifacts == null || artifacts.isEmpty()) {
            return "";
        }

        StringBuilder response = new StringBuilder();
        for (Artifact artifact : artifacts) {
            List<Part<?>> parts = artifact.parts();
            if (null == parts || parts.isEmpty()) {
                continue;
            }

            for (Part<?> part : parts) {
                String string = part.getKind().asString();
                if (StringUtils.hasText(string)) {
                    if (!response.isEmpty()) {
                        response.append("\n");
                    }
                    response.append(string);
                }
            }
        }

        return response.toString();
    }
}
