package com.assqp.platform.repository;

import com.assqp.platform.entity.SourceCodeFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SourceCodeFileRepository extends JpaRepository<SourceCodeFile, Long> {
    List<SourceCodeFile> findByProjectId(Long projectId);

    void deleteByProjectId(Long projectId);
}
