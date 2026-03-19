package com.naga.ai.orchestrator.controller;

import com.naga.ai.orchestrator.registry.AgentRegistry;
import com.naga.ai.orchestrator.service.OrchestratorResponse;
import com.naga.ai.orchestrator.service.OrchestratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orchestrator")
public class OrchestratorController {
    private static final Logger logger = LoggerFactory.getLogger(OrchestratorController.class);

    private final OrchestratorService orchestratorService;
    private final AgentRegistry agentRegistry;

    public OrchestratorController(OrchestratorService orchestratorService, AgentRegistry agentRegistry) {
        this.orchestratorService = orchestratorService;
        this.agentRegistry = agentRegistry;
    }

    @PostMapping("/requirement/start")
    public ResponseEntity<String> startRequirement(@RequestBody StartRequest request) {
        logger.info("startRequirement");

        String response = orchestratorService.startRequirement( request.requirement());
        return ResponseEntity.ok(response);
    }
    @PostMapping("/requirement/refine")
    public ResponseEntity<String> refineRequirement(@RequestBody RefineRequest request) {
        logger.info("refineRequirement — contextId: {}", request.contextId());

        String response = orchestratorService.refineRequirement(request.contextId(), request.feedback());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/requirement/approve")
    public ResponseEntity<String> approveRequirement(@RequestBody ApproveRequest request) {
        logger.info("approveRequirement — contextId: {}", request.contextId());

        String response = orchestratorService.approveRequirement(request.contextId(), request.projectKey());
        return ResponseEntity.ok(response);
    }
    @GetMapping("/agents")
    public ResponseEntity<Object> getAgents() {
        logger.info("Get All Agents ");
        return ResponseEntity.ok(agentRegistry.getAllAgents());
    }

    @PostMapping("/plan")
    public ResponseEntity<String> plan(@RequestBody PlanRequest request) {
        logger.info("POST /plan");
        String result = orchestratorService.plan(request.requirement(), request.projectKey());
        return ResponseEntity.ok(result);
    }

}
