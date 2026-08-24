package com.rrtechnosoft.lms.controller;

import com.rrtechnosoft.lms.dto.response.reports.AdminDashboardResponse;
import com.rrtechnosoft.lms.service.ReportsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Backs the admin dashboard widget the frontend already ships
 * (src/app/(admin)/admin/dashboard/page.tsx -> lib/api/dashboard.ts ->
 * GET /dashboard/admin) — that call had no backend controller behind it
 * until this Reports & Analytics module shipped. Kept as its own thin
 * controller (rather than folded into ReportsController's /reports/*
 * namespace) so the pre-existing /dashboard/admin URL contract doesn't move.
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ReportsService reportsService;

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<AdminDashboardResponse> admin() {
        return ResponseEntity.ok(reportsService.adminDashboardStats());
    }
}
