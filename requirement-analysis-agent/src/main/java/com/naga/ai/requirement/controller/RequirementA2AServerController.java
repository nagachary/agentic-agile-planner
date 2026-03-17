package com.naga.ai.requirement.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.naga.ai.requirement.model.*;
import com.naga.ai.requirement.service.RequirementService;
import io.a2a.spec.AgentCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RequirementA2AServerController {
    private static final Logger logger =
            LoggerFactory.getLogger(RequirementA2AServerController.class);

    private final AgentCard agentCard;
    private final ObjectMapper objectMapper;
    private final RequirementService requirementService;

    public RequirementA2AServerController(
            AgentCard agentCard,
            ObjectMapper objectMapper,
            RequirementService requirementService) {
        this.agentCard = agentCard;
        this.objectMapper = objectMapper;
        this.requirementService = requirementService;
    }

    /**
     * A2A discovery endpoint.
     * Orchestrator calls this to discover agent capabilities,
     * skills and communication URL before sending any tasks.
     * Standard A2A location — must be at this exact path.
     */
    @GetMapping(
            value = "/.well-known/agent-card.json",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AgentCard> getAgentCard() {
        logger.info("AgentCard: {}", agentCard.name());
        return ResponseEntity.ok(agentCard);
    }

    @PostMapping(
            value = "/",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ObjectNode> handleA2AMessage(
            @RequestBody String rawRequest) {

        logger.info("A2A message received");

        try {
            JsonNode request = objectMapper
                    .readTree(rawRequest);

            String requestId = request
                    .path("id").asText("unknown");
            JsonNode params  = request.path("params");
            String skill      = params
                    .path("skill").asText("");
            String sessionId  = params
                    .path("sessionId").asText("");
            String projectKey = params
                    .path("projectKey").asText("PLAN");
            String messageText = params
                    .path("message")
                    .path("parts")
                    .path(0)
                    .path("text")
                    .asText("");

            logger.info("A2A skill: {} sessionId: {}",
                    skill, sessionId);

            String result = switch (skill) {

                case "analyze-requirement" -> {
                    AnalysisResponse response =
                            requirementService
                                    .analyzeRequirement(
                                            messageText);
                    yield "SESSION_ID:" +
                            response.sessionId() +
                            "\n\n" +
                            response.stories();
                }

                case "refine-stories" ->
                        requirementService.refineStories(
                                messageText,
                                sessionId
                        );

                case "approve-and-store" ->
                        requirementService.approveStories(
                                sessionId,
                                projectKey
                        );

                default -> {
                    logger.warn("Unknown skill: {}", skill);
                    yield "Unknown skill: " + skill +
                            ". Supported: " +
                            "analyze-requirement, " +
                            "refine-stories, " +
                            "approve-and-store";
                }
            };

            return ResponseEntity.ok(
                    buildSuccessResponse(requestId, result));

        } catch (Exception e) {
            logger.error("A2A processing failed: {}",
                    e.getMessage(), e);
            return ResponseEntity.ok(
                    buildErrorResponse(
                            "unknown",
                            -32603,
                            "Internal error: " + e.getMessage()
                    )
            );
        }
    }
    private ObjectNode buildSuccessResponse(
            String requestId,
            String resultText) {

        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.put("id", requestId);

        ObjectNode result = objectMapper.createObjectNode();
        result.put("status", "completed");

        ObjectNode message = objectMapper.createObjectNode();
        ObjectNode part    = objectMapper.createObjectNode();
        part.put("type", "text");
        part.put("text", resultText);
        message.set("parts",
                objectMapper.createArrayNode().add(part));
        result.set("message", message);
        response.set("result", result);

        return response;
    }
    private ObjectNode buildErrorResponse(
            String requestId,
            int errorCode,
            String errorMessage) {

        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.put("id", requestId);

        ObjectNode error = objectMapper.createObjectNode();
        error.put("code", errorCode);
        error.put("message", errorMessage);
        response.set("error", error);

        return response;
    }

    @PostMapping("/requirement/analyze")
    public ResponseEntity<AnalysisResponse> analyze(
            @RequestBody AnalyzeRequest request) {

        logger.info("Received requirement: {}",
                request.requirementText());

        AnalysisResponse response = requirementService
                .analyzeRequirement(request.requirementText());

        logger.info("Analysis complete — sessionId: {}",
                response.sessionId());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/requirement/refine")
    public ResponseEntity<RefineResponse> refine(
            @RequestBody RefineRequest request) {

        logger.info("Received Refinement Request : {}",
                request.sessionId());

        String response = requirementService
                .refineStories(request.feedback(), request.sessionId());

        logger.info("Refinement completed — sessionId: {}",
                request.sessionId());

        return ResponseEntity.ok(
                new RefineResponse(
                        request.sessionId(),
                        response
                )
        );
    }

    @PostMapping("/requirement/approve")
    public ResponseEntity<ApproveResponse> approve(
            @RequestBody ApproveRequest request) {

        logger.info("Received approval for " +
                        "sessionId: {}",
                request.sessionId());

        String result = requirementService
                .approveStories(
                        request.sessionId(),
                        request.projectKey()
                );

        logger.info("Approval complete — sessionId: {}",
                request.sessionId());

        return ResponseEntity.ok(
                new ApproveResponse(
                        request.sessionId(),
                        result
                )
        );
    }
}
