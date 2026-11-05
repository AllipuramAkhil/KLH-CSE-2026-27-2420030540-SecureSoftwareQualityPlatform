package com.assqp.platform.service;

import java.util.List;
import java.util.Map;

public interface ReportService {
    Map<String, Object> generate(Long projectId, String title);
    List<Map<String, Object>> getByProject(Long projectId);
    Map<String, Object> getById(Long id);
    void delete(Long id);
}
