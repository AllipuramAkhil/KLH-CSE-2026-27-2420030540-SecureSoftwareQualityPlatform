package com.assqp.platform.analysis;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Default rule-based static analyzer. Performs lightweight lexical analysis
 * of source code to approximate quality metrics without requiring an
 * external service. This keeps the platform fully functional even when
 * no AI provider or third-party SAST tool is configured.
 */
@Component
public class RuleBasedQualityAnalyzer implements QualityAnalyzer {

    private static final Pattern METHOD_PATTERN = Pattern.compile(
            "(public|private|protected|static)[\\w<>\\[\\],\\s]*\\s+\\w+\\s*\\([^;{]*\\)\\s*\\{");

    private static final Pattern BRANCH_PATTERN = Pattern.compile(
            "\\b(if|for|while|case|catch|&&|\\|\\|)\\b");

    private static final Pattern TODO_PATTERN = Pattern.compile("(?i)\\bTODO\\b|\\bFIXME\\b");

    @Override
    public CodeMetrics analyze(String fileName, String fileContent) {
        if (fileContent == null) {
            fileContent = "";
        }

        String[] lines = fileContent.split("\\r?\\n");
        int loc = (int) java.util.Arrays.stream(lines)
                .filter(l -> !l.trim().isEmpty() && !l.trim().startsWith("//"))
                .count();

        int methods = countMatches(METHOD_PATTERN, fileContent);
        int branches = countMatches(BRANCH_PATTERN, fileContent);
        double cyclomaticComplexity = 1 + ((double) branches / Math.max(1, methods));

        int codeSmells = countMatches(TODO_PATTERN, fileContent)
                + longMethodPenalty(fileContent)
                + longLinePenalty(lines);

        int potentialBugs = countMatches(Pattern.compile("catch\\s*\\([^)]*\\)\\s*\\{\\s*\\}"), fileContent)
                + countMatches(Pattern.compile("==\\s*null\\s*\\|\\|\\s*==\\s*null"), fileContent);

        double duplicationEstimate = estimateDuplication(lines);
        double maintainability = Math.max(0, 100 - (cyclomaticComplexity * 3) - (codeSmells * 2) - (duplicationEstimate));
        double technicalDebtHours = (codeSmells * 0.5) + (potentialBugs * 1.5) + (cyclomaticComplexity > 15 ? 4 : 0);

        double overallScore = computeOverallScore(maintainability, cyclomaticComplexity, codeSmells, potentialBugs, duplicationEstimate);

        return CodeMetrics.builder()
                .linesOfCode(loc)
                .numberOfMethods(Math.max(methods, 0))
                .cyclomaticComplexity(round(cyclomaticComplexity))
                .codeDuplicationPercent(round(duplicationEstimate))
                .maintainabilityIndex(round(maintainability))
                .codeSmells(codeSmells)
                .potentialBugs(potentialBugs)
                .testCoveragePercent(0.0)
                .technicalDebtHours(round(technicalDebtHours))
                .overallQualityScore(round(overallScore))
                .build();
    }

    @Override
    public CodeMetrics aggregate(List<CodeMetrics> metricsList) {
        if (metricsList == null || metricsList.isEmpty()) {
            return CodeMetrics.builder()
                    .linesOfCode(0).numberOfMethods(0).cyclomaticComplexity(0)
                    .codeDuplicationPercent(0).maintainabilityIndex(100).codeSmells(0)
                    .potentialBugs(0).testCoveragePercent(0).technicalDebtHours(0)
                    .overallQualityScore(100).build();
        }

        int loc = metricsList.stream().mapToInt(CodeMetrics::getLinesOfCode).sum();
        int methods = metricsList.stream().mapToInt(CodeMetrics::getNumberOfMethods).sum();
        double avgComplexity = metricsList.stream().mapToDouble(CodeMetrics::getCyclomaticComplexity).average().orElse(0);
        double avgDuplication = metricsList.stream().mapToDouble(CodeMetrics::getCodeDuplicationPercent).average().orElse(0);
        double avgMaintainability = metricsList.stream().mapToDouble(CodeMetrics::getMaintainabilityIndex).average().orElse(100);
        int totalSmells = metricsList.stream().mapToInt(CodeMetrics::getCodeSmells).sum();
        int totalBugs = metricsList.stream().mapToInt(CodeMetrics::getPotentialBugs).sum();
        double totalDebt = metricsList.stream().mapToDouble(CodeMetrics::getTechnicalDebtHours).sum();
        double avgScore = metricsList.stream().mapToDouble(CodeMetrics::getOverallQualityScore).average().orElse(100);

        return CodeMetrics.builder()
                .linesOfCode(loc)
                .numberOfMethods(methods)
                .cyclomaticComplexity(round(avgComplexity))
                .codeDuplicationPercent(round(avgDuplication))
                .maintainabilityIndex(round(avgMaintainability))
                .codeSmells(totalSmells)
                .potentialBugs(totalBugs)
                .testCoveragePercent(0.0)
                .technicalDebtHours(round(totalDebt))
                .overallQualityScore(round(avgScore))
                .build();
    }

    private double computeOverallScore(double maintainability, double complexity, int smells, int bugs, double duplication) {
        double score = 100;
        score -= Math.max(0, complexity - 10) * 2;
        score -= smells * 1.5;
        score -= bugs * 4;
        score -= duplication * 0.5;
        score = (score * 0.6) + (maintainability * 0.4);
        return Math.max(0, Math.min(100, score));
    }

    private int longMethodPenalty(String content) {
        return content.length() > 8000 ? 3 : (content.length() > 4000 ? 1 : 0);
    }

    private int longLinePenalty(String[] lines) {
        int count = 0;
        for (String line : lines) {
            if (line.length() > 120) {
                count++;
            }
        }
        return Math.min(count, 20);
    }

    private double estimateDuplication(String[] lines) {
        java.util.Map<String, Integer> freq = new java.util.HashMap<>();
        int significantLines = 0;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() > 20) {
                significantLines++;
                freq.merge(trimmed, 1, Integer::sum);
            }
        }
        long duplicated = freq.values().stream().filter(v -> v > 1).mapToLong(v -> v - 1).sum();
        if (significantLines == 0) return 0;
        return Math.min(100.0, (duplicated * 100.0) / significantLines);
    }

    private int countMatches(Pattern pattern, String content) {
        Matcher matcher = pattern.matcher(content);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
