package com.assqp.platform.service;

import com.assqp.platform.dto.cicd.CiCdPipelineResponse;
import com.assqp.platform.dto.cicd.CiCdWebhookRequest;

import java.util.List;

public interface CiCdService {
    CiCdPipelineResponse handleWebhook(CiCdWebhookRequest request);
    CiCdPipelineResponse getById(Long id);
    List<CiCdPipelineResponse> getByProject(Long projectId);
    CiCdPipelineResponse trigger(Long projectId);
}
