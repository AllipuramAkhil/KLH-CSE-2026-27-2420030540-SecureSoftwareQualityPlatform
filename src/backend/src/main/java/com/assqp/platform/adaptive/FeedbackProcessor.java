package com.assqp.platform.adaptive;

import com.assqp.platform.entity.AnalysisHistory;
import com.assqp.platform.entity.Project;
import com.assqp.platform.repository.AnalysisHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Records the outcome of each testing/analysis cycle into AnalysisHistory
 * so that subsequent adaptive testing decisions (via TestPriorityCalculator)
 * can factor in historical trends. This is the mechanism that closes the
 * continuous feedback loop described in the project architecture.
 */
@Component
@RequiredArgsConstructor
public class FeedbackProcessor {

    private final AnalysisHistoryRepository analysisHistoryRepository;

    public void recordCycle(Project project, String cycleType, String summary,
                             Double qualityScore, Double securityScore, Double riskScore) {
        AnalysisHistory history = AnalysisHistory.builder()
                .project(project)
                .cycleType(cycleType)
                .summary(summary)
                .qualityScore(qualityScore)
                .securityScore(securityScore)
                .riskScore(riskScore)
                .build();
        analysisHistoryRepository.save(history);
    }
}
