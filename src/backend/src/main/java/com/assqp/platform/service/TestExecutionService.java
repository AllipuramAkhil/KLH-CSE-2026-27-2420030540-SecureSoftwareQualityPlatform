package com.assqp.platform.service;

import com.assqp.platform.dto.testing.TestExecutionRequest;
import com.assqp.platform.dto.testing.TestExecutionResponse;

import java.util.List;

public interface TestExecutionService {
    TestExecutionResponse execute(Long testCaseId, TestExecutionRequest request);
    List<TestExecutionResponse> getByTestCase(Long testCaseId);
    List<TestExecutionResponse> getByProject(Long projectId);
    TestExecutionResponse getById(Long id);
}
