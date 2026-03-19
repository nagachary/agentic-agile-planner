package com.naga.ai.requirement.controller;

import com.naga.ai.requirement.model.*;
import com.naga.ai.requirement.service.RequirementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RequirementA2AServerController {
    private static final Logger logger = LoggerFactory.getLogger(RequirementA2AServerController.class);

    private final RequirementService requirementService;

    public RequirementA2AServerController(RequirementService requirementService) {
        this.requirementService = requirementService;
    }

    @PostMapping("/requirement/analyze")
    public ResponseEntity<AnalysisResponse> analyze(@RequestBody AnalyzeRequest request) {
        logger.info("Received requirement: {}", request.requirementText());

        AnalysisResponse response = requirementService.analyzeRequirement(request.requirementText());
        logger.info("Analysis complete — sessionId: {}",response.sessionId());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/requirement/refine")
    public ResponseEntity<RefineResponse> refine(
            @RequestBody RefineRequest request) {
        logger.info("Received Refinement Request : {}", request.sessionId());

        String response = requirementService.refineStories(request.feedback(), request.sessionId());
        logger.info("Refinement completed — sessionId: {}", request.sessionId());

        return ResponseEntity.ok(new RefineResponse(request.sessionId(),response)
        );
    }

    @PostMapping("/requirement/approve")
    public ResponseEntity<ApproveResponse> approve(@RequestBody ApproveRequest request) {
        logger.info("Received approval for sessionId: {}", request.sessionId());

        String result = requirementService.approveStories(request.sessionId(),request.projectKey());
        logger.info("Approval complete — sessionId: {}", request.sessionId());

        return ResponseEntity.ok(new ApproveResponse( request.sessionId(),result));
    }
}
