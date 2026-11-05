package com.assqp.platform.repository;

import com.assqp.platform.entity.AnalysisHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisHistoryRepository extends JpaRepository<AnalysisHistory, Long> {
    List<AnalysisHistory> findByProjectIdOrderByRecordedAtDesc(Long projectId);

    void deleteByProjectId(Long projectId);
}
