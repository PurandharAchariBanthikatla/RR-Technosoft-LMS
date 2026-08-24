package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.CreateAdminRequest;
import com.rrtechnosoft.lms.dto.response.UserSummaryResponse;
import com.rrtechnosoft.lms.entity.AdminProfile;
import com.rrtechnosoft.lms.entity.User;
import com.rrtechnosoft.lms.entity.enums.AccountStatus;
import com.rrtechnosoft.lms.entity.enums.UserRole;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.AdminProfileRepository;
import com.rrtechnosoft.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Business rule enforced here (in addition to the DB check for the single
 * Super Admin row): the platform allows at most {@link #MAX_ADMINS} ADMIN
 * accounts, provisioned only by the Super Admin.
 */
@Service
@RequiredArgsConstructor
public class AdminManagementService {

    private static final int MAX_ADMINS = 10;

    private final UserRepository userRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Transactional
    public UserSummaryResponse createAdmin(CreateAdminRequest request, UUID createdBy) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw ApiException.conflict("An account with this email already exists");
        }
        long currentAdmins = userRepository.countByRole(UserRole.ADMIN);
        if (currentAdmins >= MAX_ADMINS) {
            throw ApiException.conflict("Maximum of " + MAX_ADMINS + " Admin accounts already reached");
        }

        User admin = User.builder()
                .role(UserRole.ADMIN)
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .phone(request.phone())
                .status(AccountStatus.ACTIVE)
                .createdBy(createdBy)
                .build();
        admin = userRepository.save(admin);

        AdminProfile profile = AdminProfile.builder()
                .userId(admin.getId())
                .user(admin)
                .department(request.department())
                .designation(request.designation())
                .assignedBy(createdBy)
                .build();
        adminProfileRepository.save(profile);

        auditLogService.log(createdBy, "CREATE_ADMIN", "User", admin.getId(), null);
        return UserSummaryResponse.from(admin);
    }

    @Transactional
    public UserSummaryResponse setAdminStatus(UUID adminId, AccountStatus status, UUID actorId) {
        User admin = userRepository.findById(adminId)
                .filter(u -> u.getRole() == UserRole.ADMIN)
                .orElseThrow(() -> ApiException.notFound("Admin not found"));
        admin.setStatus(status);
        userRepository.save(admin);
        auditLogService.log(actorId, "UPDATE_ADMIN_STATUS_" + status, "User", adminId, null);
        return UserSummaryResponse.from(admin);
    }

    @Transactional
    public void deleteAdmin(UUID adminId, UUID actorId) {
        User admin = userRepository.findById(adminId)
                .filter(u -> u.getRole() == UserRole.ADMIN)
                .orElseThrow(() -> ApiException.notFound("Admin not found"));
        userRepository.delete(admin);
        auditLogService.log(actorId, "DELETE_ADMIN", "User", adminId, null);
    }

    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> listAdmins(Pageable pageable) {
        return userRepository.findByRole(UserRole.ADMIN, pageable).map(UserSummaryResponse::from);
    }
}
