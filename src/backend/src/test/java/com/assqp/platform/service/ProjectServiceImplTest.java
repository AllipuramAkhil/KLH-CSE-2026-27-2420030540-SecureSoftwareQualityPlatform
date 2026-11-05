package com.assqp.platform.service;

import com.assqp.platform.dto.project.ProjectRequest;
import com.assqp.platform.dto.project.ProjectResponse;
import com.assqp.platform.entity.Project;
import com.assqp.platform.entity.User;
import com.assqp.platform.enums.Role;
import com.assqp.platform.exception.ForbiddenException;
import com.assqp.platform.exception.ResourceNotFoundException;
import com.assqp.platform.repository.AiRecommendationRepository;
import com.assqp.platform.repository.AnalysisHistoryRepository;
import com.assqp.platform.repository.CiCdPipelineRepository;
import com.assqp.platform.repository.CodeChurnMetricsRepository;
import com.assqp.platform.repository.CoverageReportRepository;
import com.assqp.platform.repository.MlPredictionLogRepository;
import com.assqp.platform.repository.ProjectRepository;
import com.assqp.platform.repository.QualityAnalysisRepository;
import com.assqp.platform.repository.ReportRepository;
import com.assqp.platform.repository.RiskAssessmentRepository;
import com.assqp.platform.repository.SecurityVulnerabilityRepository;
import com.assqp.platform.repository.SonarQubeAnalysisRepository;
import com.assqp.platform.repository.SourceCodeFileRepository;
import com.assqp.platform.repository.TestCaseRepository;
import com.assqp.platform.repository.TestExecutionRepository;
import com.assqp.platform.repository.UserRepository;
import com.assqp.platform.security.CurrentUserProvider;
import com.assqp.platform.security.UserPrincipal;
import com.assqp.platform.service.impl.ProjectServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;

    // --- Phase 12 audit fix: delete() now explicitly cleans up every child
    // table before deleting the project (see ProjectServiceImpl) - all of
    // these must be mocked or @InjectMocks leaves them null and delete()
    // NPEs the moment it calls one.
    @Mock
    private AiRecommendationRepository aiRecommendationRepository;
    @Mock
    private AnalysisHistoryRepository analysisHistoryRepository;
    @Mock
    private CiCdPipelineRepository ciCdPipelineRepository;
    @Mock
    private CodeChurnMetricsRepository codeChurnMetricsRepository;
    @Mock
    private CoverageReportRepository coverageReportRepository;
    @Mock
    private MlPredictionLogRepository mlPredictionLogRepository;
    @Mock
    private QualityAnalysisRepository qualityAnalysisRepository;
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private RiskAssessmentRepository riskAssessmentRepository;
    @Mock
    private SecurityVulnerabilityRepository securityVulnerabilityRepository;
    @Mock
    private SonarQubeAnalysisRepository sonarQubeAnalysisRepository;
    @Mock
    private SourceCodeFileRepository sourceCodeFileRepository;
    @Mock
    private TestCaseRepository testCaseRepository;
    @Mock
    private TestExecutionRepository testExecutionRepository;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private User owner;
    private UserPrincipal ownerPrincipal;

    private void stubOwner() {
        owner = User.builder().id(1L).fullName("Owner").email("owner@test.com")
                .password("x").role(Role.DEVELOPER).enabled(true).build();
        ownerPrincipal = new UserPrincipal(owner);
        lenient().when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        lenient().when(currentUserProvider.getCurrentUser()).thenReturn(ownerPrincipal);
    }

    @Test
    void create_validRequest_savesAndReturnsProject() {
        stubOwner();
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            p.setId(10L);
            return p;
        });

        ProjectRequest request = ProjectRequest.builder().name("ASSQP").description("desc").build();
        ProjectResponse response = projectService.create(request);

        assertThat(response.getName()).isEqualTo("ASSQP");
        assertThat(response.getOwnerName()).isEqualTo("Owner");
    }

    @Test
    void getById_notFound_throwsResourceNotFound() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_notOwnerAndNotAdmin_throwsForbidden() {
        stubOwner();
        User otherOwner = User.builder().id(2L).fullName("Other").email("other@test.com")
                .password("x").role(Role.DEVELOPER).enabled(true).build();
        Project project = Project.builder().id(5L).name("Other project").owner(otherOwner).build();

        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.getById(5L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getAll_nonAdmin_returnsOnlyOwnProjects() {
        stubOwner();
        Pageable pageable = PageRequest.of(0, 10);
        Project project = Project.builder().id(1L).name("Mine").owner(owner).build();
        when(projectRepository.findAllByOwnerId(1L, pageable)).thenReturn(new PageImpl<>(List.of(project)));

        var page = projectService.getAll(pageable);

        assertThat(page.getContent()).hasSize(1);
        verify(projectRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void delete_ownedProject_deletesSuccessfully() {
        stubOwner();
        Project project = Project.builder().id(5L).name("Mine").owner(owner).build();
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));

        projectService.delete(5L);

        // Every child table must be cleaned up before the project row itself
        // is deleted, or this would throw a FK constraint violation against
        // a real database - see ProjectServiceImpl.delete().
        verify(testExecutionRepository).deleteByTestCaseProjectId(5L);
        verify(testCaseRepository).deleteByProjectId(5L);
        verify(aiRecommendationRepository).deleteByProjectId(5L);
        verify(analysisHistoryRepository).deleteByProjectId(5L);
        verify(ciCdPipelineRepository).deleteByProjectId(5L);
        verify(codeChurnMetricsRepository).deleteByProjectId(5L);
        verify(coverageReportRepository).deleteByProjectId(5L);
        verify(mlPredictionLogRepository).deleteByProjectId(5L);
        verify(qualityAnalysisRepository).deleteByProjectId(5L);
        verify(reportRepository).deleteByProjectId(5L);
        verify(riskAssessmentRepository).deleteByProjectId(5L);
        verify(securityVulnerabilityRepository).deleteByProjectId(5L);
        verify(sonarQubeAnalysisRepository).deleteByProjectId(5L);
        verify(sourceCodeFileRepository).deleteByProjectId(5L);
        verify(projectRepository).delete(project);
    }
}
