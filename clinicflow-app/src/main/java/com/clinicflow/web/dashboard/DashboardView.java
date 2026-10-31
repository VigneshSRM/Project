package com.clinicflow.web.dashboard;

import java.util.List;

public record DashboardView(
        String title,
        String subtitle,
        String primaryActionLabel,
        String primaryActionHref,
        List<KpiCard> kpis,
        String activityTitle,
        List<ActivityRow> activity
) {
}