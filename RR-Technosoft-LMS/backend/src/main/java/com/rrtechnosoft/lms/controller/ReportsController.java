package com.rrtechnosoft.lms.controller;

import com.rrtechnosoft.lms.dto.response.reports.*;
import com.rrtechnosoft.lms.service.ReportsService;
import com.rrtechnosoft.lms.service.export.ExcelExportService;
import com.rrtechnosoft.lms.service.export.PdfExportService;
import com.rrtechnosoft.lms.service.export.ReportExportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Reports & Analytics module. Every endpoint here is read-only and
 * restricted to SUPER_ADMIN/ADMIN (see SecurityConfig — same tier as
 * Attendance/Live Classes/Student management).
 * <p>
 * Each list report follows the same pair of endpoints: a paginated JSON
 * endpoint the frontend renders as a data table, and one or two
 * {@code /export/*} endpoints that re-run the same query unpaged (capped —
 * see {@link #EXPORT_CAP}) and stream back a formatted file. This keeps the
 * exported file's rows identical to whatever the current filters show on
 * screen, just without pagination.
 */
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
public class ReportsController {

    /** Hard cap on rows pulled into memory for a single export — generous for an LMS's scale, not unbounded. */
    private static final int EXPORT_CAP = 20_000;

    private static final DateTimeFormatter GENERATED_AT_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");

    private final ReportsService reportsService;
    private final ExcelExportService excelExportService;
    private final PdfExportService pdfExportService;
    private final ReportExportMapper mapper;

    // =====================================================================
    // Dashboard Analytics
    // =====================================================================

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardAnalyticsResponse> dashboard() {
        return ResponseEntity.ok(reportsService.dashboardAnalytics());
    }

    // =====================================================================
    // Student Reports
    // =====================================================================

    @GetMapping("/students")
    public ResponseEntity<Page<StudentReportRowResponse>> students(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String batch,
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) UUID courseId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(reportsService.studentReport(search, batch, branch, courseId, pageable));
    }

    @GetMapping("/students/export/excel")
    public ResponseEntity<byte[]> studentsExportExcel(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String batch,
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) UUID courseId) {
        List<StudentReportRowResponse> rows = reportsService
                .studentReport(search, batch, branch, courseId, exportPageable("fullName")).getContent();
        byte[] file = excelExportService.export("Student Report", filterSummary(search, batch, branch),
                mapper.studentHeaders(), mapper.studentRows(rows));
        return excelResponse(file, "student-report");
    }

    @GetMapping("/students/export/pdf")
    public ResponseEntity<byte[]> studentsExportPdf(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String batch,
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) UUID courseId) {
        List<StudentReportRowResponse> rows = reportsService
                .studentReport(search, batch, branch, courseId, exportPageable("fullName")).getContent();
        byte[] file = pdfExportService.export("Student Report", filterSummary(search, batch, branch),
                mapper.studentHeaders(), mapper.studentRows(rows));
        return pdfResponse(file, "student-report");
    }

    // =====================================================================
    // Faculty Reports
    // =====================================================================

    @GetMapping("/faculty")
    public ResponseEntity<Page<FacultyReportRowResponse>> faculty(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(reportsService.facultyReport(search, pageable));
    }

    @GetMapping("/faculty/export/excel")
    public ResponseEntity<byte[]> facultyExportExcel(@RequestParam(required = false) String search) {
        List<FacultyReportRowResponse> rows = reportsService.facultyReport(search, exportPageable("instructorName")).getContent();
        byte[] file = excelExportService.export("Faculty Report", filterSummary(search, null, null),
                mapper.facultyHeaders(), mapper.facultyRows(rows));
        return excelResponse(file, "faculty-report");
    }

    @GetMapping("/faculty/export/pdf")
    public ResponseEntity<byte[]> facultyExportPdf(@RequestParam(required = false) String search) {
        List<FacultyReportRowResponse> rows = reportsService.facultyReport(search, exportPageable("instructorName")).getContent();
        byte[] file = pdfExportService.export("Faculty Report", filterSummary(search, null, null),
                mapper.facultyHeaders(), mapper.facultyRows(rows));
        return pdfResponse(file, "faculty-report");
    }

    // =====================================================================
    // Attendance Reports
    // =====================================================================

    @GetMapping("/attendance")
    public ResponseEntity<Page<AttendanceReportRowResponse>> attendance(
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(reportsService.attendanceReport(courseId, from, to, pageable));
    }

    @GetMapping("/attendance/export/excel")
    public ResponseEntity<byte[]> attendanceExportExcel(
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<AttendanceReportRowResponse> rows = reportsService
                .attendanceReport(courseId, from, to, exportPageable("courseTitle")).getContent();
        byte[] file = excelExportService.export("Attendance Report", dateRangeSummary(from, to),
                mapper.attendanceHeaders(), mapper.attendanceRows(rows));
        return excelResponse(file, "attendance-report");
    }

    @GetMapping("/attendance/export/pdf")
    public ResponseEntity<byte[]> attendanceExportPdf(
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<AttendanceReportRowResponse> rows = reportsService
                .attendanceReport(courseId, from, to, exportPageable("courseTitle")).getContent();
        byte[] file = pdfExportService.export("Attendance Report", dateRangeSummary(from, to),
                mapper.attendanceHeaders(), mapper.attendanceRows(rows));
        return pdfResponse(file, "attendance-report");
    }

    // =====================================================================
    // Assignment Reports
    // =====================================================================

    @GetMapping("/assignments")
    public ResponseEntity<Page<AssignmentReportRowResponse>> assignments(
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(reportsService.assignmentReport(courseId, from, to, pageable));
    }

    @GetMapping("/assignments/export/excel")
    public ResponseEntity<byte[]> assignmentsExportExcel(
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        List<AssignmentReportRowResponse> rows = reportsService
                .assignmentReport(courseId, from, to, exportPageable("dueAt")).getContent();
        byte[] file = excelExportService.export("Assignment Report", filterSummary(null, null, null),
                mapper.assignmentHeaders(), mapper.assignmentRows(rows));
        return excelResponse(file, "assignment-report");
    }

    @GetMapping("/assignments/export/pdf")
    public ResponseEntity<byte[]> assignmentsExportPdf(
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        List<AssignmentReportRowResponse> rows = reportsService
                .assignmentReport(courseId, from, to, exportPageable("dueAt")).getContent();
        byte[] file = pdfExportService.export("Assignment Report", filterSummary(null, null, null),
                mapper.assignmentHeaders(), mapper.assignmentRows(rows));
        return pdfResponse(file, "assignment-report");
    }

    // =====================================================================
    // Revenue Reports
    // =====================================================================

    @GetMapping("/revenue")
    public ResponseEntity<Page<RevenueReportRowResponse>> revenue(
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(reportsService.revenueReport(courseId, from, to, pageable));
    }

    @GetMapping("/revenue/summary")
    public ResponseEntity<RevenueSummaryResponse> revenueSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        return ResponseEntity.ok(reportsService.revenueSummary(from, to));
    }

    @GetMapping("/revenue/export/excel")
    public ResponseEntity<byte[]> revenueExportExcel(
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        List<RevenueReportRowResponse> rows = reportsService
                .revenueReport(courseId, from, to, exportPageable("courseTitle")).getContent();
        byte[] file = excelExportService.export("Revenue Report", filterSummary(null, null, null),
                mapper.revenueHeaders(), mapper.revenueRows(rows));
        return excelResponse(file, "revenue-report");
    }

    @GetMapping("/revenue/export/pdf")
    public ResponseEntity<byte[]> revenueExportPdf(
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        List<RevenueReportRowResponse> rows = reportsService
                .revenueReport(courseId, from, to, exportPageable("courseTitle")).getContent();
        byte[] file = pdfExportService.export("Revenue Report", filterSummary(null, null, null),
                mapper.revenueHeaders(), mapper.revenueRows(rows));
        return pdfResponse(file, "revenue-report");
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private Pageable exportPageable(String sortProperty) {
        return PageRequest.of(0, EXPORT_CAP, Sort.by(sortProperty).ascending());
    }

    private String filterSummary(String search, String batch, String branch) {
        StringBuilder sb = new StringBuilder("Generated " + LocalDateTime.now().format(GENERATED_AT_FMT));
        if (search != null && !search.isBlank()) sb.append(" · search=\"").append(search).append('"');
        if (batch != null && !batch.isBlank()) sb.append(" · batch=").append(batch);
        if (branch != null && !branch.isBlank()) sb.append(" · branch=").append(branch);
        return sb.toString();
    }

    private String dateRangeSummary(LocalDate from, LocalDate to) {
        StringBuilder sb = new StringBuilder("Generated " + LocalDateTime.now().format(GENERATED_AT_FMT));
        if (from != null) sb.append(" · from=").append(from);
        if (to != null) sb.append(" · to=").append(to);
        return sb.toString();
    }

    private ResponseEntity<byte[]> excelResponse(byte[] file, String filenamePrefix) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(attachment(filenamePrefix, "xlsx"));
        return new ResponseEntity<>(file, headers, org.springframework.http.HttpStatus.OK);
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] file, String filenamePrefix) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(attachment(filenamePrefix, "pdf"));
        return new ResponseEntity<>(file, headers, org.springframework.http.HttpStatus.OK);
    }

    private ContentDisposition attachment(String prefix, String ext) {
        String stamp = OffsetDateTime.now(ZoneOffset.UTC).toLocalDate().toString();
        return ContentDisposition.attachment().filename(prefix + "-" + stamp + "." + ext).build();
    }
}
