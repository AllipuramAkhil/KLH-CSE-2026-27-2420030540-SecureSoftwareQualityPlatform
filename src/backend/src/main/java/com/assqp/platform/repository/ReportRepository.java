package com.assqp.platform.repository;

import com.assqp.platform.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByProjectIdOrderByGeneratedAtDesc(Long projectId);

    void deleteByProjectId(Long projectId);
}
