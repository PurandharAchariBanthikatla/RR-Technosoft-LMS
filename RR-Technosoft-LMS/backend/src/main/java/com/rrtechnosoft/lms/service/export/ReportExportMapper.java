package com.rrtechnosoft.lms.service.export;

import com.rrtechnosoft.lms.dto.response.reports.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Flattens each Reports & Analytics DTO into the generic
 * (headers, List&lt;List&lt;Object&gt;&gt;) shape {@link ExcelExportService} and
 * {@link PdfExportService} render — kept as one small mapper so both export
 * formats always show the same columns for the same report.
 */
@Component
public class ReportExportMapper {

    public List<String> studentHeaders() {
        return List.of("Student ID", "Name", "Email", "Batch", "Branch", "College",
                "Courses Enrolled", "Avg Progress %", "Attendance %", "Avg Assignment Score",
                "Assignments Submitted", "Assignments Pending");
    }

    public List<List<Object>> studentRows(List<StudentReportRowResponse> rows) {
        List<List<Object>> out = new ArrayList<>();
        for (StudentReportRowResponse r : rows) {
            out.add(List.of(
                    nullSafe(r.studentCode()), r.fullName(), nullSafe(r.email()),
                    nullSafe(r.batch()), nullSafe(r.branch()), nullSafe(r.college()),
                    r.coursesEnrolled(), r.avgProgressPercentage(), r.attendancePercentage(),
                    r.avgAssignmentScore(), r.assignmentsSubmitted(), r.assignmentsPending()
            ));
        }
        return out;
    }

    public List<String> facultyHeaders() {
        return List.of("Instructor", "Courses Handled", "Total Students", "Avg Rating",
                "Avg Completion %", "Revenue Generated");
    }

    public List<List<Object>> facultyRows(List<FacultyReportRowResponse> rows) {
        List<List<Object>> out = new ArrayList<>();
        for (FacultyReportRowResponse r : rows) {
            out.add(List.of(r.instructorName(), r.coursesHandled(), r.totalStudents(),
                    r.avgCourseRating(), r.avgCompletionPercentage(), r.revenueGenerated().doubleValue()));
        }
        return out;
    }

    public List<String> attendanceHeaders() {
        return List.of("Course", "Sessions Held", "Present", "Absent", "Late", "Excused", "Attendance %");
    }

    public List<List<Object>> attendanceRows(List<AttendanceReportRowResponse> rows) {
        List<List<Object>> out = new ArrayList<>();
        for (AttendanceReportRowResponse r : rows) {
            out.add(List.of(r.courseTitle(), r.sessionsHeld(), r.presentCount(), r.absentCount(),
                    r.lateCount(), r.excusedCount(), r.attendancePercentage()));
        }
        return out;
    }

    public List<String> assignmentHeaders() {
        return List.of("Assignment", "Course", "Due Date", "Total Students", "Submitted",
                "Graded", "Late", "Pending", "Avg Score", "Submission Rate %");
    }

    public List<List<Object>> assignmentRows(List<AssignmentReportRowResponse> rows) {
        List<List<Object>> out = new ArrayList<>();
        for (AssignmentReportRowResponse r : rows) {
            out.add(List.of(r.assignmentTitle(), nullSafe(r.courseTitle()),
                    r.dueAt() == null ? "-" : r.dueAt(), r.totalStudents(), r.submittedCount(),
                    r.gradedCount(), r.lateCount(), r.pendingCount(), r.avgScore(), r.submissionRatePercentage()));
        }
        return out;
    }

    public List<String> revenueHeaders() {
        return List.of("Course", "Category", "Unit Price (₹)", "Paid Enrollments",
                "Dropped/Pending Enrollments", "Total Revenue (₹)");
    }

    public List<List<Object>> revenueRows(List<RevenueReportRowResponse> rows) {
        List<List<Object>> out = new ArrayList<>();
        for (RevenueReportRowResponse r : rows) {
            out.add(List.of(r.courseTitle(), nullSafe(r.category()), r.unitPrice().doubleValue(),
                    r.paidEnrollments(), r.droppedOrPendingEnrollments(), r.totalRevenue().doubleValue()));
        }
        return out;
    }

    private static Object nullSafe(String s) {
        return s == null ? "-" : s;
    }
}
