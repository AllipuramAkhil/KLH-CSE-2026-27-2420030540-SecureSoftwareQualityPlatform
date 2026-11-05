package com.assqp.platform.adaptive;

import com.assqp.platform.dto.testing.AdaptiveTestRecommendation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Sorts and selects test cases for execution based on their computed
 * priority scores, returning the recommended execution order.
 */
@Component
@RequiredArgsConstructor
public class TestSelectionEngine {

    public List<AdaptiveTestRecommendation> orderByPriority(List<AdaptiveTestRecommendation> recommendations) {
        return recommendations.stream()
                .sorted(Comparator.comparingDouble(AdaptiveTestRecommendation::getPriorityScore).reversed())
                .toList();
    }
}
