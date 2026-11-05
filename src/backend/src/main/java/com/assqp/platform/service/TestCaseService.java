package com.assqp.platform.service;

import com.assqp.platform.dto.testing.TestCaseRequest;
import com.assqp.platform.dto.testing.TestCaseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TestCaseService {
    TestCaseResponse create(Long projectId, TestCaseRequest request);
    Page<TestCaseResponse> getByProject(Long projectId, Pageable pageable);
    Page<TestCaseResponse> getByProjectFiltered(Long projectId, String type, String priority, String status, Pageable pageable);
    TestCaseResponse getById(Long id);
    TestCaseResponse update(Long id, TestCaseRequest request);
    TestCaseResponse updateStatus(Long id, String status);
    void delete(Long id);
}
