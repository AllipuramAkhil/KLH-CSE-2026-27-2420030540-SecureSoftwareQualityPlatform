package com.assqp.platform.adaptive;

import com.assqp.platform.entity.RiskAssessment;
import com.assqp.platform.repository.RiskAssessmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Resolves the current risk score for a given component name, used by the
 * adaptive testing engine to weight test priority.
 */
@Component
@RequiredArgsConstructor
public class RiskBasedPrioritizer {

    private final RiskAssessmentRepository riskAssessmentRepository;

    public double resolveComponentRisk(Long projectId, String componentName) {
        if (componentName == null || componentName.isBlank()) {
            return 0.0;
        }
        List<RiskAssessment> assessments = riskAssessmentRepository.findByProjectIdOrderByAssessedAtDesc(projectId);
        return assessments.stream()
                .filter(r -> componentName.equalsIgnoreCase(r.getComponentName()))
                .findFirst()
                .map(RiskAssessment::getRiskScore)
                .orElse(0.0);
    }
}
