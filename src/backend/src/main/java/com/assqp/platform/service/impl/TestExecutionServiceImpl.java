package com.assqp.platform.service.impl;

import com.assqp.platform.dto.testing.TestExecutionRequest;
import com.assqp.platform.dto.testing.TestExecutionResponse;
import com.assqp.platform.entity.TestCase;
import com.assqp.platform.entity.TestExecution;
import com.assqp.platform.enums.ExecutionStatus;
import com.assqp.platform.exception.BadRequestException;
import com.assqp.platform.exception.ResourceNotFoundException;
import com.assqp.platform.repository.TestCaseRepository;
import com.assqp.platform.repository.TestExecutionRepository;
import com.assqp.platform.security.ProjectAccessGuard;
import com.assqp.platform.service.TestExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TestExecutionServiceImpl implements TestExecutionService {

    private final TestExecutionRepository testExecutionRepository;
    private final TestCaseRepository testCaseRepository;
    private final ProjectAccessGuard projectAccessGuard;

    @Override
    @Transactional
    public TestExecutionResponse execute(Long testCaseId, TestExecutionRequest request) {
        TestCase testCase = testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Test case not found with id: " + testCaseId));
        projectAccessGuard.checkAccess(testCase.getProject()); // ownership check

        ExecutionStatus status = parseStatus(request.getStatus());
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusNanos((request.getDurationMs() != null ? request.getDurationMs() : 0) * 1_000_000);

        TestExecution execution = TestExecution.builder()
                .testCase(testCase)
                .status(status)
                .startTime(start)
                .endTime(end)
                .durationMs(request.getDurationMs())
                .result(request.getResult())
                .logs(request.getLogs())
                .errorMessage(request.getErrorMessage())
                .environment(request.getEnvironment())
                .buildNumber(request.getBuildNumber())
                .build();

        execution = testExecutionRepository.save(execution);

        // Update the test case's rolling historical failure rate and last execution timestamp,
        // which directly feeds the adaptive testing engine's prioritization logic.
        updateHistoricalFailureRate(testCase, status);
        testCase.setLastExecution(end);
        testCase.setActualResult(request.getResult());
        testCaseRepository.save(testCase);

        return toResponse(execution);
    }

    @Override
    public List<TestExecutionResponse> getByTestCase(Long testCaseId) {
        TestCase testCase = testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Test case not found with id: " + testCaseId));
        projectAccessGuard.checkAccess(testCase.getProject()); // ownership check
        return testExecutionRepository.findByTestCaseIdOrderByCreatedAtDesc(testCaseId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<TestExecutionResponse> getByProject(Long projectId) {
        projectAccessGuard.getAccessibleProject(projectId); // ownership check
        return testExecutionRepository.findByTestCaseProjectIdOrderByCreatedAtDesc(projectId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public TestExecutionResponse getById(Long id) {
        TestExecution execution = testExecutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Test execution not found with id: " + id));
        projectAccessGuard.checkAccess(execution.getTestCase().getProject()); // ownership check
        return toResponse(execution);
    }

    private void updateHistoricalFailureRate(TestCase testCase, ExecutionStatus latestStatus) {
        List<TestExecution> history = testExecutionRepository.findByTestCaseIdOrderByCreatedAtDesc(testCase.getId());
        long total = history.size() + 1; // include the execution just recorded
        long failures = history.stream()
                .filter(e -> e.getStatus() == ExecutionStatus.FAILED || e.getStatus() == ExecutionStatus.ERROR)
                .count() + (latestStatus == ExecutionStatus.FAILED || latestStatus == ExecutionStatus.ERROR ? 1 : 0);
        double rate = total == 0 ? 0.0 : (double) failures / total;
        testCase.setHistoricalFailureRate(Math.round(rate * 100.0) / 100.0);
    }

    private ExecutionStatus parseStatus(String value) {
        try {
            return ExecutionStatus.valueOf(value.toUpperCase());
        } catch (Exception ex) {
            throw new BadRequestException("Invalid execution status: " + value);
        }
    }

    private TestExecutionResponse toResponse(TestExecution e) {
        return TestExecutionResponse.builder()
                .id(e.getId())
                .testCaseId(e.getTestCase().getId())
                .status(e.getStatus().name())
                .startTime(e.getStartTime())
                .endTime(e.getEndTime())
                .durationMs(e.getDurationMs())
                .result(e.getResult())
                .logs(e.getLogs())
                .errorMessage(e.getErrorMessage())
                .environment(e.getEnvironment())
                .buildNumber(e.getBuildNumber())
                .build();
    }
}
