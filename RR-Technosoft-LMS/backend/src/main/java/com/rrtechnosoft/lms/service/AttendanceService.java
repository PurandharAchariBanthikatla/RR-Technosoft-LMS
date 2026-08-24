package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.BulkMarkAttendanceRequest;
import com.rrtechnosoft.lms.dto.request.MarkAttendanceRequest;
import com.rrtechnosoft.lms.dto.response.AttendanceResponse;
import com.rrtechnosoft.lms.dto.response.AttendanceSummaryResponse;
import com.rrtechnosoft.lms.entity.Attendance;
import com.rrtechnosoft.lms.entity.Course;
import com.rrtechnosoft.lms.entity.User;
import com.rrtechnosoft.lms.entity.enums.AttendanceStatus;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.AttendanceRepository;
import com.rrtechnosoft.lms.repository.CourseRepository;
import com.rrtechnosoft.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> list(UUID courseId, LocalDate date, Pageable pageable) {
        return attendanceRepository.search(courseId, date, pageable).map(AttendanceResponse::from);
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> mine(UUID studentId, LocalDate from, LocalDate to) {
        return attendanceRepository.findForStudent(studentId, from, to).stream()
                .map(AttendanceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AttendanceSummaryResponse summary(UUID studentId) {
        long total = attendanceRepository.countByStudentId(studentId);
        long present = attendanceRepository.countByStudentIdAndStatus(studentId, AttendanceStatus.PRESENT);
        long absent = attendanceRepository.countByStudentIdAndStatus(studentId, AttendanceStatus.ABSENT);
        long late = attendanceRepository.countByStudentIdAndStatus(studentId, AttendanceStatus.LATE);
        double percentage = total == 0 ? 0.0 : Math.round((present * 10000.0) / total) / 100.0;
        return new AttendanceSummaryResponse(total, present, absent, late, percentage);
    }

    @Transactional
    public AttendanceResponse mark(MarkAttendanceRequest request, UUID actorId) {
        Attendance record = upsert(request.courseId(), request.studentId(), request.date(), request.status(), actorId);
        auditLogService.log(actorId, "MARK_ATTENDANCE", "Attendance", record.getId(), null);
        return AttendanceResponse.from(record);
    }

    @Transactional
    public List<AttendanceResponse> bulkMark(BulkMarkAttendanceRequest request, UUID actorId) {
        List<AttendanceResponse> results = request.records().stream()
                .map(r -> upsert(request.courseId(), r.studentId(), request.date(), r.status(), actorId))
                .map(AttendanceResponse::from)
                .toList();
        auditLogService.log(actorId, "BULK_MARK_ATTENDANCE", "Course", request.courseId(), null);
        return results;
    }

    private Attendance upsert(UUID courseId, UUID studentId, LocalDate date, AttendanceStatus status, UUID actorId) {
        Attendance record = attendanceRepository.findByCourseIdAndStudentIdAndAttendanceDate(courseId, studentId, date)
                .orElseGet(() -> {
                    Course course = courseRepository.findById(courseId)
                            .orElseThrow(() -> ApiException.notFound("Course not found"));
                    User student = userRepository.findById(studentId)
                            .orElseThrow(() -> ApiException.notFound("Student not found"));
                    return Attendance.builder()
                            .course(course)
                            .student(student)
                            .attendanceDate(date)
                            .build();
                });
        record.setStatus(status);
        record.setMarkedBy(actorId);
        record.setMarkedAt(OffsetDateTime.now());
        return attendanceRepository.save(record);
    }
}
