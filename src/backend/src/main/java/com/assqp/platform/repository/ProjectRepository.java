package com.assqp.platform.repository;

import com.assqp.platform.entity.Project;
import com.assqp.platform.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    Page<Project> findByOwner(User owner, Pageable pageable);
    Page<Project> findAllByOwnerId(Long ownerId, Pageable pageable);

    // Phase 12 audit fix: Project.owner is a nullable=false FK - used to
    // block deleting a user who still owns projects (see UserServiceImpl.delete())
    // instead of letting that fail with a raw FK-constraint SQL exception.
    boolean existsByOwnerId(Long ownerId);
}
