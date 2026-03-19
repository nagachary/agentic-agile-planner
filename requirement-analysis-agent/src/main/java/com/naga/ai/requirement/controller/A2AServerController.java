package com.naga.ai.requirement.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.naga.ai.requirement.model.AnalysisResponse;
import com.naga.ai.requirement.service.RequirementService;
import io.a2a.spec.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class A2AServerController {
    private static final Logger logger = LoggerFactory.getLogger( A2AServerController.class);

    private final AgentCard agentCard;
    private final RequirementService requirementService;
    private final ObjectMapper objectMapper;

    public A2AServerController( AgentCard agentCard, RequirementService requirementService, ObjectMapper objectMapper) {
        this.agentCard = agentCard;
        this.requirementService = requirementService;
        this.objectMapper = objectMapper;
    }

    @GetMapping(value = "/.well-known/agent-card.json",produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AgentCard> getAgentCard() {
        logger.info("AgentCard: {}", agentCard.name());

        return ResponseEntity.ok(agentCard);
    }


    @PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ObjectNode> handleA2AMessage(@RequestBody JsonNode request) {

        //logger.info("A2A message — contextId: {} messageId: {}",message.contextId(), message.messageId());
        logger.info("A2A request received — method: {}",request.path("method").asText());
        String requestId = request.path("id").asText("1");

        try {
            JsonNode messageNode = request.path("params").path("message");
            String contextId = messageNode.path("contextId").asText(null);
            String messageText = "";
            JsonNode parts = messageNode.path("parts");
            if (parts.isArray() && !parts.isEmpty()) {
                messageText = parts.path(0).path("text").asText("");
            }
            logger.info("A2A message — contextId : {} - text: {}", contextId, messageText);
            //String messageText = extractText(message);
            // String contextId = message.contextId();
            String resultText;
            String taskContextId;

            if (contextId == null || contextId.isBlank()) {
                logger.info("New session — analyzing");
                AnalysisResponse response = requirementService.analyzeRequirement(messageText);
                taskContextId = response.sessionId();
                resultText = response.stories();

            } else if (messageText.contains("Approved projectKey:")) {
                // Approval → create Epic in Jira
                logger.info("Approval — contextId: {}", contextId);
                String projectKey = messageText.replace("Approved projectKey:", "").trim();
                resultText = requirementService.approveStories(contextId, projectKey);
                taskContextId = contextId;

            } else {
                // Follow-up → refine stories
                logger.info("Refinement — contextId: {}", contextId);
                resultText = requirementService.refineStories(messageText, contextId);
                taskContextId = contextId;
            }

            Task task = buildTask(taskContextId, resultText);
            logger.info("Returning task — contextId: {}", taskContextId);

            ObjectNode response = objectMapper.createObjectNode();
            response.put("jsonrpc", "2.0");
            response.put("id", requestId);
            response.set("result", objectMapper.valueToTree(task));

            logger.info("Returning task — contextId: {}",taskContextId);
            String responseJson = objectMapper.writeValueAsString(response);
            logger.info("Response JSON: {}", responseJson);

            return ResponseEntity.ok(response);
        }catch (Exception exception) {
            logger.error("A2A processing failed: {}", exception.getMessage(), exception);

            // Return JSON-RPC error envelope
            ObjectNode errorResponse = objectMapper.createObjectNode();
            errorResponse.put("jsonrpc", "2.0");
            errorResponse.put("id", requestId);

            ObjectNode error = objectMapper.createObjectNode();
            error.put("code", -32603);
            error.put("message","Internal error: " + exception.getMessage());
            errorResponse.set("error", error);

            return ResponseEntity.ok(errorResponse);
        }
    }


    private Task buildTask( String contextId, String resultText) {

        return Task.builder()
                .id(UUID.randomUUID().toString())
                .contextId(contextId)
                .status(new TaskStatus(
                        TaskState.COMPLETED))
                .artifacts(List.of(Artifact.builder().artifactId(UUID.randomUUID().toString()).parts(List.of(new TextPart(resultText))).build())).build();
    }

    private String extractText(Message message) {
        if (message.parts() == null || message.parts().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Part<?> part : message.parts()) {
            if (part instanceof TextPart tp) {
                if (tp.text() != null && !tp.text().isBlank()) {
                    sb.append(tp.text());
                }
            }
        }
        return sb.toString();
    }
}