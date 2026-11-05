package com.assqp.platform.service;

import com.assqp.platform.dto.project.ProjectRequest;
import com.assqp.platform.dto.project.ProjectResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProjectService {
    ProjectResponse create(ProjectRequest request);
    Page<ProjectResponse> getAll(Pageable pageable);
    ProjectResponse getById(Long id);
    ProjectResponse update(Long id, ProjectRequest request);
    void delete(Long id);
}
