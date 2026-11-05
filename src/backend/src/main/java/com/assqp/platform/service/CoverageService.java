package com.assqp.platform.service;

import com.assqp.platform.dto.coverage.CoverageReportResponse;
import com.assqp.platform.dto.coverage.CoverageTrendPoint;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CoverageService {

    /**
     * Imports the JaCoCo report from the locally configured path
     * (coverage.jacoco.report-path). Intended for local/dev use, or for a
     * CI step running on the same filesystem as the backend.
     */
    CoverageReportResponse importFromDefaultPath(Long projectId);

    /**
     * Imports a JaCoCo XML report uploaded directly (e.g. by a GitHub
     * Actions step that has the report as a build artifact but not
     * filesystem access to this backend).
     */
    CoverageReportResponse importFromFile(Long projectId, MultipartFile file);

    CoverageReportResponse getLatest(Long projectId);

    List<CoverageReportResponse> getHistory(Long projectId);

    List<CoverageTrendPoint> getTrend(Long projectId);
}
