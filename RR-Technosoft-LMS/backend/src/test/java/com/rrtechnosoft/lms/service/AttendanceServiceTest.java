package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.MarkAttendanceRequest;
import com.rrtechnosoft.lms.dto.response.AttendanceResponse;
import com.rrtechnosoft.lms.dto.response.AttendanceSummaryResponse;
import com.rrtechnosoft.lms.entity.Attendance;
import com.rrtechnosoft.lms.entity.Course;
import com.rrtechnosoft.lms.entity.User;
import com.rrtechnosoft.lms.entity.enums.AttendanceStatus;
import com.rrtechnosoft.lms.repository.AttendanceRepository;
import com.rrtechnosoft.lms.repository.CourseRepository;
import com.rrtechnosoft.lms.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock private AttendanceRepository attendanceRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private AttendanceService attendanceService;

    private final UUID actorId = UUID.randomUUID();
    private final UUID courseId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();

    @Test
    void mark_createsNewRecordWhenNoneExistsForThatDay() {
        var request = new MarkAttendanceRequest(studentId, courseId, LocalDate.now(), AttendanceStatus.PRESENT);
        when(attendanceRepository.findByCourseIdAndStudentIdAndAttendanceDate(courseId, studentId, request.date()))
                .thenReturn(Optional.empty());
        when(courseRepository.findById(courseId))
                .thenReturn(Optional.of(Course.builder().id(courseId).title("DevOps Track").build()));
        when(userRepository.findById(studentId))
                .thenReturn(Optional.of(User.builder().id(studentId).fullName("Ravi Kumar").build()));
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> {
            Attendance a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        AttendanceResponse response = attendanceService.mark(request, actorId);

        assertThat(response.status()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(response.studentName()).isEqualTo("Ravi Kumar");
    }

    @Test
    void mark_updatesExistingRecordInsteadOfDuplicating() {
        LocalDate date = LocalDate.now();
        Attendance existing = Attendance.builder()
                .id(UUID.randomUUID())
                .course(Course.builder().id(courseId).title("DevOps Track").build())
                .student(User.builder().id(studentId).fullName("Ravi Kumar").build())
                .attendanceDate(date)
                .status(AttendanceStatus.ABSENT)
                .build();
        when(attendanceRepository.findByCourseIdAndStudentIdAndAttendanceDate(courseId, studentId, date))
                .thenReturn(Optional.of(existing));
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

        AttendanceResponse response = attendanceService.mark(
                new MarkAttendanceRequest(studentId, courseId, date, AttendanceStatus.LATE), actorId);

        assertThat(response.status()).isEqualTo(AttendanceStatus.LATE);
        assertThat(existing.getMarkedBy()).isEqualTo(actorId);
    }

    @Test
    void summary_computesPercentageFromPresentCount() {
        when(attendanceRepository.countByStudentId(studentId)).thenReturn(20L);
        when(attendanceRepository.countByStudentIdAndStatus(studentId, AttendanceStatus.PRESENT)).thenReturn(18L);
        when(attendanceRepository.countByStudentIdAndStatus(studentId, AttendanceStatus.ABSENT)).thenReturn(2L);
        when(attendanceRepository.countByStudentIdAndStatus(studentId, AttendanceStatus.LATE)).thenReturn(0L);

        AttendanceSummaryResponse summary = attendanceService.summary(studentId);

        assertThat(summary.totalClasses()).isEqualTo(20);
        assertThat(summary.percentage()).isEqualTo(90.0);
    }
}
