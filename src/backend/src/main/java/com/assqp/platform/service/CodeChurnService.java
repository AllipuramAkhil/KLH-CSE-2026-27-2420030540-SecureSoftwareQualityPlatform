package com.assqp.platform.service;

import com.assqp.platform.dto.churn.CodeChurnAnalysisResponse;
import com.assqp.platform.dto.churn.CodeChurnResponse;
import com.assqp.platform.dto.churn.GitChurnStatusResponse;

import java.util.List;

public interface CodeChurnService {
    GitChurnStatusResponse getStatus();

    CodeChurnAnalysisResponse analyze(Long projectId);

    List<CodeChurnResponse> getByProject(Long projectId);
}
