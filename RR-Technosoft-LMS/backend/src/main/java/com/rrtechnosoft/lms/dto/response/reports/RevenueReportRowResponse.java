package com.rrtechnosoft.lms.dto.response.reports;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One row of the Revenue Report, grouped by course. Revenue is derived from
 * paid enrollments (status ACTIVE or COMPLETED) x course price — there is no
 * separate payments/invoices table in the schema, so this is the schema's
 * source of truth for "who paid for what" (see ReportsService javadoc).
 */
public record RevenueReportRowResponse(
        UUID courseId,
        String courseTitle,
        String category,
        BigDecimal unitPrice,
        long paidEnrollments,
        long droppedOrPendingEnrollments,
        BigDecimal totalRevenue
) {}
