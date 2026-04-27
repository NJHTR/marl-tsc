package com.njhtr.marltsc.web.service.api;

import com.njhtr.marltsc.web.api.dto.response.DashboardResponse;
import com.njhtr.marltsc.web.api.dto.response.IntersectionSummaryResponse;
import com.njhtr.marltsc.web.api.dto.response.OptimizeResponse;

import java.util.List;

public interface DashboardAppService {

    DashboardResponse getDashboardData(String intersectionId);

    List<IntersectionSummaryResponse> listIntersections();

    OptimizeResponse triggerOptimization(String intersectionId, String strategy);
}
