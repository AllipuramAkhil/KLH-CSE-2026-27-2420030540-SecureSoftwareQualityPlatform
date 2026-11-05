package com.assqp.platform.service;

import com.assqp.platform.dto.dashboard.DashboardResponse;

public interface DashboardService {
    DashboardResponse getDashboard(Long projectId);
}
