package com.agentopscrm.controller;

import com.agentopscrm.dto.EvaluationRequest;
import com.agentopscrm.dto.EvaluationResponse;
import com.agentopscrm.service.EvaluationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for the Evaluation Agent (F-008).
 *
 * Currently exposes a single test endpoint that lets developers verify the
 * Evaluation Agent's verdict (grounding / hallucination-risk / safety) for an
 * arbitrary question + draft answer + retrieved chunks, without going through
 * the full chat flow. The heavy lifting lives in {@link EvaluationService}; the
 * controller stays thin.
 *
 * @author AgentOps Team
 * @version 0.8.0
 */
@RestController
@RequestMapping("/api/evaluation")
@CrossOrigin(origins = "*")
public class EvaluationController {

    private static final Logger logger = LoggerFactory.getLogger(EvaluationController.class);

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    /**
     * POST /api/evaluation/test
     * Evaluate a draft answer against retrieved chunks and return the verdict.
     */
    @PostMapping("/test")
    public ResponseEntity<EvaluationResponse> test(@Valid @RequestBody EvaluationRequest request) {
        logger.info("POST /api/evaluation/test - businessId: {}, chunks: {}",
                request.getBusinessId(),
                request.getRetrievedChunks() == null ? 0 : request.getRetrievedChunks().size());

        EvaluationResponse response = evaluationService.evaluate(request);
        return ResponseEntity.ok(response);
    }
}
