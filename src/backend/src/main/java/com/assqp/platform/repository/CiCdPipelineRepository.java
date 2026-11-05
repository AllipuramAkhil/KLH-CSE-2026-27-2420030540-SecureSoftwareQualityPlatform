package com.assqp.platform.repository;

import com.assqp.platform.entity.CiCdPipeline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CiCdPipelineRepository extends JpaRepository<CiCdPipeline, Long> {
    List<CiCdPipeline> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    void deleteByProjectId(Long projectId);
}
