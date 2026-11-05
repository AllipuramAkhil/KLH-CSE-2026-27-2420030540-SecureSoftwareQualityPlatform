package com.assqp.platform.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface SourceCodeService {
    Map<String, Object> upload(Long projectId, MultipartFile file);
    List<Map<String, Object>> listByProject(Long projectId);
    Map<String, Object> getById(Long fileId);
    void delete(Long fileId);
}
