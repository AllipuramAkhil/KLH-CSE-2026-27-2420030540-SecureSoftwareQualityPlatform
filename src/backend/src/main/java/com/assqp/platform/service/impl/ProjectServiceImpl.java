package com.assqp.platform.service.impl;

import com.assqp.platform.dto.project.ProjectRequest;
import com.assqp.platform.dto.project.ProjectResponse;
import com.assqp.platform.entity.Project;
import com.assqp.platform.entity.User;
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
import com.assqp.platform.security.ProjectAccessGuard;
import com.assqp.platform.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final ProjectAccessGuard projectAccessGuard;

    // --- Phase 12 audit fix: every one of these holds a nullable=false FK to
    // Project with no DB-level or JPA-level cascade configured anywhere. Before
    // this fix, delete() below called projectRepository.delete(project) directly,
    // which would throw a DataIntegrityViolationException (FK constraint) for
    // any project that actually had data - i.e. any real project. See delete().
    private final AiRecommendationRepository aiRecommendationRepository;
    private final AnalysisHistoryRepository analysisHistoryRepository;
    private final CiCdPipelineRepository ciCdPipelineRepository;
    private final CodeChurnMetricsRepository codeChurnMetricsRepository;
    private final CoverageReportRepository coverageReportRepository;
    private final MlPredictionLogRepository mlPredictionLogRepository;
    private final QualityAnalysisRepository qualityAnalysisRepository;
    private final ReportRepository reportRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final SecurityVulnerabilityRepository securityVulnerabilityRepository;
    private final SonarQubeAnalysisRepository sonarQubeAnalysisRepository;
    private final SourceCodeFileRepository sourceCodeFileRepository;
    private final TestCaseRepository testCaseRepository;
    private final TestExecutionRepository testExecutionRepository;

    @Override
    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        User owner = userRepository.findById(currentUserProvider.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .repositoryUrl(request.getRepositoryUrl())
                .branch(request.getBranch())
                .technologyStack(request.getTechnologyStack())
                .owner(owner)
                .qualityScore(0.0)
                .securityScore(0.0)
                .riskScore(0.0)
                .build();

        project = projectRepository.save(project);
        return toResponse(project);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProjectResponse> getAll(Pageable pageable) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        boolean isAdmin = currentUserProvider.getCurrentUser().getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_ADMIN"));

        Page<Project> projects = isAdmin
                ? projectRepository.findAll(pageable)
                : projectRepository.findAllByOwnerId(currentUserId, pageable);

        return projects.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getById(Long id) {
        Project project = getOwnedProjectOrThrow(id);
        return toResponse(project);
    }

    @Override
    @Transactional
    public ProjectResponse update(Long id, ProjectRequest request) {
        Project project = getOwnedProjectOrThrow(id);
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setRepositoryUrl(request.getRepositoryUrl());
        project.setBranch(request.getBranch());
        project.setTechnologyStack(request.getTechnologyStack());
        project = projectRepository.save(project);
        return toResponse(project);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Project project = getOwnedProjectOrThrow(id);

        // TestExecution references Project only indirectly (via TestCase), so
        // it must be cleared before TestCase itself is deleted. Every other
        // child here has a direct FK to Project and no cross-dependencies on
        // each other, so their order relative to one another doesn't matter.
        testExecutionRepository.deleteByTestCaseProjectId(id);
        testCaseRepository.deleteByProjectId(id);

        aiRecommendationRepository.deleteByProjectId(id);
        analysisHistoryRepository.deleteByProjectId(id);
        ciCdPipelineRepository.deleteByProjectId(id);
        codeChurnMetricsRepository.deleteByProjectId(id);
        coverageReportRepository.deleteByProjectId(id);
        mlPredictionLogRepository.deleteByProjectId(id);
        qualityAnalysisRepository.deleteByProjectId(id);
        reportRepository.deleteByProjectId(id);
        riskAssessmentRepository.deleteByProjectId(id);
        securityVulnerabilityRepository.deleteByProjectId(id);
        sonarQubeAnalysisRepository.deleteByProjectId(id);
        sourceCodeFileRepository.deleteByProjectId(id);

        projectRepository.delete(project);
    }

    private Project getOwnedProjectOrThrow(Long id) {
        return projectAccessGuard.getAccessibleProject(id);
    }

    private ProjectResponse toResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .repositoryUrl(project.getRepositoryUrl())
                .branch(project.getBranch())
                .technologyStack(project.getTechnologyStack())
                .status(project.getStatus().name())
                .ownerName(project.getOwner() != null ? project.getOwner().getFullName() : null)
                .qualityScore(project.getQualityScore())
                .securityScore(project.getSecurityScore())
                .riskScore(project.getRiskScore())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
