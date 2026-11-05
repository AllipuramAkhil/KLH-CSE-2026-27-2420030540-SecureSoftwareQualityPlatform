package com.assqp.platform.service.impl;

import com.assqp.platform.dto.cicd.CiCdPipelineResponse;
import com.assqp.platform.dto.cicd.CiCdWebhookRequest;
import com.assqp.platform.entity.CiCdPipeline;
import com.assqp.platform.entity.Project;
import com.assqp.platform.enums.PipelineStatus;
import com.assqp.platform.exception.ResourceNotFoundException;
import com.assqp.platform.repository.CiCdPipelineRepository;
import com.assqp.platform.repository.ProjectRepository;
import com.assqp.platform.security.ProjectAccessGuard;
import com.assqp.platform.service.CiCdService;
import com.assqp.platform.service.QualityAnalysisService;
import com.assqp.platform.service.RiskAssessmentService;
import com.assqp.platform.service.SecurityAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Handles inbound CI/CD webhook events. On each triggered pipeline, the
 * platform automatically runs quality analysis, security analysis, and
 * risk assessment - enabling continuous quality/security monitoring as
 * part of the CI/CD flow, per the project requirements.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CiCdServiceImpl implements CiCdService {

    private final CiCdPipelineRepository pipelineRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessGuard projectAccessGuard;
    private final QualityAnalysisService qualityAnalysisService;
    private final SecurityAnalysisService securityAnalysisService;
    private final RiskAssessmentService riskAssessmentService;

    @Override
    @Transactional
    public CiCdPipelineResponse handleWebhook(CiCdWebhookRequest request) {
        // NOTE: this path is reached by the public webhook endpoint, which is authenticated
        // by a shared secret/signature (see CiCdController), not by a logged-in user - there
        // is no SecurityContext user to check ownership against here. Callers that DO have an
        // authenticated user (e.g. trigger() below) must enforce ownership before calling this.
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + request.getProjectId()));

        CiCdPipeline pipeline = CiCdPipeline.builder()
                .project(project)
                .branch(request.getBranch())
                .commitHash(request.getCommitHash())
                .buildNumber(request.getBuildNumber())
                .status(PipelineStatus.RUNNING)
                .triggerSource(request.getTriggerSource())
                .startTime(LocalDateTime.now())
                .build();
        pipeline = pipelineRepository.save(pipeline);

        try {
            qualityAnalysisService.analyze(project.getId());
            securityAnalysisService.analyze(project.getId());
            riskAssessmentService.analyze(project.getId());
            pipeline.setStatus(PipelineStatus.SUCCESS);
        } catch (Exception ex) {
            log.error("CI/CD triggered analysis failed for project {}: {}", project.getId(), ex.getMessage());
            pipeline.setStatus(PipelineStatus.FAILED);
        }

        pipeline.setEndTime(LocalDateTime.now());
        pipeline = pipelineRepository.save(pipeline);

        return toResponse(pipeline);
    }

    @Override
    @Transactional
    public CiCdPipelineResponse trigger(Long projectId) {
        projectAccessGuard.getAccessibleProject(projectId); // ownership check - this endpoint IS user-authenticated

        CiCdWebhookRequest request = CiCdWebhookRequest.builder()
                .projectId(projectId)
                .branch("manual")
                .commitHash("N/A")
                .buildNumber("manual-" + System.currentTimeMillis())
                .triggerSource("manual-trigger")
                .build();
        return handleWebhook(request);
    }

    @Override
    public CiCdPipelineResponse getById(Long id) {
        CiCdPipeline pipeline = pipelineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pipeline not found with id: " + id));
        projectAccessGuard.checkAccess(pipeline.getProject()); // ownership check
        return toResponse(pipeline);
    }

    @Override
    public List<CiCdPipelineResponse> getByProject(Long projectId) {
        projectAccessGuard.getAccessibleProject(projectId); // ownership check
        return pipelineRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream().map(this::toResponse).toList();
    }

    private CiCdPipelineResponse toResponse(CiCdPipeline p) {
        return CiCdPipelineResponse.builder()
                .id(p.getId())
                .projectId(p.getProject().getId())
                .branch(p.getBranch())
                .commitHash(p.getCommitHash())
                .buildNumber(p.getBuildNumber())
                .status(p.getStatus().name())
                .triggerSource(p.getTriggerSource())
                .startTime(p.getStartTime())
                .endTime(p.getEndTime())
                .build();
    }
}
