package com.assqp.platform.security;

import com.assqp.platform.entity.Project;
import com.assqp.platform.exception.ForbiddenException;
import com.assqp.platform.exception.ResourceNotFoundException;
import com.assqp.platform.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * Security fix (broken access control / IDOR): ProjectServiceImpl had a private
 * getOwnedProjectOrThrow() ownership check, but every other project-scoped service
 * (security, quality, coverage, risk, testing, CI/CD, source code, reports, SonarQube,
 * Trivy, ZAP, AI analysis, dashboard, adaptive testing, code churn) called
 * projectRepository.findById(id) directly and skipped it. Any authenticated user who
 * knew or guessed another user's project id could read or trigger analysis on it.
 * <p>
 * This component centralizes that check so every service can share one implementation
 * instead of each duplicating (or forgetting) it. Ownership rule: ADMIN can access any
 * project; everyone else must be the project's owner.
 */
@Component
@RequiredArgsConstructor
public class ProjectAccessGuard {

    private final ProjectRepository projectRepository;
    private final CurrentUserProvider currentUserProvider;

    /**
     * Loads a project by id and enforces ownership. Use this everywhere a project-scoped
     * service previously called {@code projectRepository.findById(id)} directly.
     *
     * @throws ResourceNotFoundException if no project with this id exists
     * @throws ForbiddenException        if the current user is neither an admin nor the owner
     */
    public Project getAccessibleProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        if (!isAdmin() && !isOwner(project)) {
            throw new ForbiddenException("You do not have access to this project");
        }
        return project;
    }

    /** Same check for callers that already have a loaded Project and just need to verify access. */
    public void checkAccess(Project project) {
        if (!isAdmin() && !isOwner(project)) {
            throw new ForbiddenException("You do not have access to this project");
        }
    }

    private boolean isOwner(Project project) {
        return project.getOwner() != null
                && project.getOwner().getId().equals(currentUserProvider.getCurrentUserId());
    }

    private boolean isAdmin() {
        return currentUserProvider.getCurrentUser().getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
