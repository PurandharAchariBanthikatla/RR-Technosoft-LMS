package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.CreateStudentRequest;
import com.rrtechnosoft.lms.dto.request.UpdateStudentRequest;
import com.rrtechnosoft.lms.dto.response.UserSummaryResponse;
import com.rrtechnosoft.lms.entity.StudentProfile;
import com.rrtechnosoft.lms.entity.User;
import com.rrtechnosoft.lms.entity.enums.AccountStatus;
import com.rrtechnosoft.lms.entity.enums.UserRole;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.StudentProfileRepository;
import com.rrtechnosoft.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentManagementService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentIdGenerator studentIdGenerator;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Transactional
    public UserSummaryResponse createStudent(CreateStudentRequest request, UUID createdBy) {
        String studentId = studentIdGenerator.next();

        User student = User.builder()
                .role(UserRole.STUDENT)
                .studentId(studentId)
                .passwordHash(passwordEncoder.encode(request.initialPassword()))
                .fullName(request.fullName())
                .phone(request.phone())
                .status(AccountStatus.ACTIVE)
                .createdBy(createdBy)
                .build();
        student = userRepository.save(student);

        StudentProfile profile = StudentProfile.builder()
                .userId(student.getId())
                .user(student)
                .batch(request.batch())
                .branch(request.branch())
                .college(request.college())
                .graduationYear(request.graduationYear())
                .build();
        studentProfileRepository.save(profile);

        auditLogService.log(createdBy, "CREATE_STUDENT", "User", student.getId(), null);
        return UserSummaryResponse.from(student);
    }

    /**
     * Edits an existing student's profile (name/phone/academic fields).
     * Reuses the same User + StudentProfile entities createStudent wrote —
     * no new tables, no change to studentId/status/password, which are
     * each already owned by their own endpoint.
     *
     * Fields are only overwritten when present in the request (fullName is
     * the exception — it's required, same as create). The list screen this
     * ships against doesn't return batch/branch/college/graduationYear, so
     * an edit submitted without touching those fields must never blank out
     * data the admin never saw or meant to change — "preserve existing
     * data" from the production-safety checklist.
     */
    @Transactional
    public UserSummaryResponse updateStudent(UUID studentId, UpdateStudentRequest request, UUID actorId) {
        User student = userRepository.findById(studentId)
                .filter(u -> u.getRole() == UserRole.STUDENT)
                .orElseThrow(() -> ApiException.notFound("Student not found"));
        student.setFullName(request.fullName());
        if (request.phone() != null) {
            student.setPhone(request.phone());
        }
        userRepository.save(student);

        StudentProfile profile = studentProfileRepository.findByUserId(studentId)
                .orElseGet(() -> StudentProfile.builder().userId(studentId).user(student).build());
        if (request.batch() != null) profile.setBatch(request.batch());
        if (request.branch() != null) profile.setBranch(request.branch());
        if (request.college() != null) profile.setCollege(request.college());
        if (request.graduationYear() != null) profile.setGraduationYear(request.graduationYear());
        studentProfileRepository.save(profile);

        auditLogService.log(actorId, "UPDATE_STUDENT", "User", studentId, null);
        return UserSummaryResponse.from(student);
    }

    @Transactional
    public UserSummaryResponse setStudentStatus(UUID studentId, AccountStatus status, UUID actorId) {
        User student = userRepository.findById(studentId)
                .filter(u -> u.getRole() == UserRole.STUDENT)
                .orElseThrow(() -> ApiException.notFound("Student not found"));
        student.setStatus(status);
        userRepository.save(student);
        auditLogService.log(actorId, "UPDATE_STUDENT_STATUS_" + status, "User", studentId, null);
        return UserSummaryResponse.from(student);
    }

    @Transactional
    public void deleteStudent(UUID studentId, UUID actorId) {
        User student = userRepository.findById(studentId)
                .filter(u -> u.getRole() == UserRole.STUDENT)
                .orElseThrow(() -> ApiException.notFound("Student not found"));
        userRepository.delete(student);
        auditLogService.log(actorId, "DELETE_STUDENT", "User", studentId, null);
    }

    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> listStudents(String searchName, Pageable pageable) {
        if (searchName == null || searchName.isBlank()) {
            return userRepository.findByRole(UserRole.STUDENT, pageable).map(UserSummaryResponse::from);
        }
        return userRepository.findByRoleAndFullNameContainingIgnoreCase(UserRole.STUDENT, searchName, pageable)
                .map(UserSummaryResponse::from);
    }
}
