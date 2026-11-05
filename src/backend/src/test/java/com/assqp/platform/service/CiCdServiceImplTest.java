package com.assqp.platform.service;

import com.assqp.platform.dto.cicd.CiCdPipelineResponse;
import com.assqp.platform.dto.cicd.CiCdWebhookRequest;
import com.assqp.platform.entity.CiCdPipeline;
import com.assqp.platform.entity.Project;
import com.assqp.platform.enums.PipelineStatus;
import com.assqp.platform.repository.CiCdPipelineRepository;
import com.assqp.platform.repository.ProjectRepository;
import com.assqp.platform.service.impl.CiCdServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CiCdServiceImplTest {

    @Mock
    private CiCdPipelineRepository pipelineRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private QualityAnalysisService qualityAnalysisService;
    @Mock
    private SecurityAnalysisService securityAnalysisService;
    @Mock
    private RiskAssessmentService riskAssessmentService;

    @InjectMocks
    private CiCdServiceImpl ciCdService;

    @Test
    void handleWebhook_successfulAnalysis_marksPipelineSuccess() {
        Project project = Project.builder().id(1L).build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(pipelineRepository.save(any(CiCdPipeline.class))).thenAnswer(inv -> {
            CiCdPipeline p = inv.getArgument(0);
            if (p.getId() == null) p.setId(1L);
            return p;
        });

        CiCdWebhookRequest request = CiCdWebhookRequest.builder()
                .projectId(1L).branch("main").commitHash("abc123").buildNumber("42").triggerSource("github-actions")
                .build();

        CiCdPipelineResponse response = ciCdService.handleWebhook(request);

        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        verify(qualityAnalysisService).analyze(1L);
        verify(securityAnalysisService).analyze(1L);
        verify(riskAssessmentService).analyze(1L);
    }

    @Test
    void handleWebhook_analysisThrows_marksPipelineFailed() {
        Project project = Project.builder().id(1L).build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(pipelineRepository.save(any(CiCdPipeline.class))).thenAnswer(inv -> {
            CiCdPipeline p = inv.getArgument(0);
            if (p.getId() == null) p.setId(1L);
            return p;
        });
        doThrow(new RuntimeException("Analysis failed")).when(qualityAnalysisService).analyze(1L);

        CiCdWebhookRequest request = CiCdWebhookRequest.builder().projectId(1L).branch("main").build();

        CiCdPipelineResponse response = ciCdService.handleWebhook(request);

        assertThat(response.getStatus()).isEqualTo("FAILED");
    }

    @Test
    void trigger_delegatesToWebhookHandling() {
        Project project = Project.builder().id(1L).build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(pipelineRepository.save(any(CiCdPipeline.class))).thenAnswer(inv -> {
            CiCdPipeline p = inv.getArgument(0);
            if (p.getId() == null) p.setId(2L);
            return p;
        });

        CiCdPipelineResponse response = ciCdService.trigger(1L);

        assertThat(response.getTriggerSource()).isEqualTo("manual-trigger");
    }
}
