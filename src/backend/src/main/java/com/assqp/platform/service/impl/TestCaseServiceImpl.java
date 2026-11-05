package com.assqp.platform.service.impl;

import com.assqp.platform.dto.testing.TestCaseRequest;
import com.assqp.platform.dto.testing.TestCaseResponse;
import com.assqp.platform.entity.Project;
import com.assqp.platform.entity.TestCase;
import com.assqp.platform.enums.TestType;
import com.assqp.platform.exception.BadRequestException;
import com.assqp.platform.exception.ResourceNotFoundException;
import com.assqp.platform.enums.TestCaseStatus;
import com.assqp.platform.repository.ProjectRepository;
import com.assqp.platform.security.ProjectAccessGuard;
import com.assqp.platform.repository.TestCaseRepository;
import com.assqp.platform.repository.TestExecutionRepository;
import com.assqp.platform.service.TestCaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TestCaseServiceImpl implements TestCaseService {

    private final TestCaseRepository testCaseRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessGuard projectAccessGuard;
    private final TestExecutionRepository testExecutionRepository;

    @Override
    @Transactional
    public TestCaseResponse create(Long projectId, TestCaseRequest request) {
        Project project = projectAccessGuard.getAccessibleProject(projectId);

        TestCase testCase = TestCase.builder()
                .project(project)
                .title(request.getTitle())
                .description(request.getDescription())
                .testType(parseTestType(request.getTestType()))
                .priority(request.getPriority() != null ? request.getPriority() : "MEDIUM")
                .preconditions(request.getPreconditions())
                .testSteps(request.getTestSteps())
                .expectedResult(request.getExpectedResult())
                .relatedComponent(request.getRelatedComponent())
                .historicalFailureRate(0.0)
                .riskScore(0.0)
                .build();

        testCase = testCaseRepository.save(testCase);
        return toResponse(testCase);
    }

    @Override
    public Page<TestCaseResponse> getByProject(Long projectId, Pageable pageable) {
        projectAccessGuard.getAccessibleProject(projectId); // ownership check
        return testCaseRepository.findByProjectId(projectId, pageable).map(this::toResponse);
    }

    @Override
    public Page<TestCaseResponse> getByProjectFiltered(Long projectId, String type, String priority, String status, Pageable pageable) {
        projectAccessGuard.getAccessibleProject(projectId); // ownership check
        List<TestCase> filtered = testCaseRepository.findByProjectId(projectId).stream()
                .filter(t -> type == null || t.getTestType().name().equalsIgnoreCase(type))
                .filter(t -> priority == null || (t.getPriority() != null && t.getPriority().equalsIgnoreCase(priority)))
                .filter(t -> status == null || t.getStatus().name().equalsIgnoreCase(status))
                .toList();

        int start = (int) Math.min(pageable.getOffset(), filtered.size());
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        List<TestCaseResponse> pageContent = filtered.subList(start, end).stream().map(this::toResponse).toList();

        return new PageImpl<>(pageContent, pageable, filtered.size());
    }

    @Override
    public TestCaseResponse getById(Long id) {
        return toResponse(getEntity(id));
    }

    @Override
    @Transactional
    public TestCaseResponse updateStatus(Long id, String status) {
        TestCase testCase = getEntity(id);
        try {
            testCase.setStatus(TestCaseStatus.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid test case status: " + status);
        }
        testCase = testCaseRepository.save(testCase);
        return toResponse(testCase);
    }

    @Override
    @Transactional
    public TestCaseResponse update(Long id, TestCaseRequest request) {
        TestCase testCase = getEntity(id);
        testCase.setTitle(request.getTitle());
        testCase.setDescription(request.getDescription());
        testCase.setTestType(parseTestType(request.getTestType()));
        if (request.getPriority() != null) testCase.setPriority(request.getPriority());
        testCase.setPreconditions(request.getPreconditions());
        testCase.setTestSteps(request.getTestSteps());
        testCase.setExpectedResult(request.getExpectedResult());
        testCase.setRelatedComponent(request.getRelatedComponent());
        testCase = testCaseRepository.save(testCase);
        return toResponse(testCase);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        TestCase testCase = getEntity(id);
        // Phase 12 audit fix: TestExecution.testCase is a nullable=false FK -
        // deleting a test case with any execution history would otherwise
        // fail with a raw FK-constraint SQL exception.
        testExecutionRepository.deleteByTestCaseId(id);
        testCaseRepository.delete(testCase);
    }

    private TestCase getEntity(Long id) {
        TestCase testCase = testCaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Test case not found with id: " + id));
        projectAccessGuard.checkAccess(testCase.getProject()); // ownership check
        return testCase;
    }

    private TestType parseTestType(String value) {
        try {
            return TestType.valueOf(value.toUpperCase());
        } catch (Exception ex) {
            throw new BadRequestException("Invalid test type: " + value);
        }
    }

    private TestCaseResponse toResponse(TestCase t) {
        return TestCaseResponse.builder()
                .id(t.getId())
                .projectId(t.getProject().getId())
                .title(t.getTitle())
                .description(t.getDescription())
                .testType(t.getTestType().name())
                .priority(t.getPriority())
                .preconditions(t.getPreconditions())
                .testSteps(t.getTestSteps())
                .expectedResult(t.getExpectedResult())
                .actualResult(t.getActualResult())
                .status(t.getStatus().name())
                .relatedComponent(t.getRelatedComponent())
                .historicalFailureRate(t.getHistoricalFailureRate())
                .riskScore(t.getRiskScore())
                .lastExecution(t.getLastExecution())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
