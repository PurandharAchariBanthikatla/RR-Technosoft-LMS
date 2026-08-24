package com.rrtechnosoft.lms.controller;

import com.rrtechnosoft.lms.dto.request.UpdateProfileRequest;
import com.rrtechnosoft.lms.dto.response.UserSummaryResponse;
import com.rrtechnosoft.lms.entity.User;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.UserRepository;
import com.rrtechnosoft.lms.security.UserPrincipal;
import com.rrtechnosoft.lms.service.AuditLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Self-service "my profile" endpoints — any authenticated user (Student,
 * Admin, or Super Admin) editing their own name/phone. Previously missing
 * entirely: the frontend's /profile page called nothing real and just
 * faked a success toast after a timeout. This gives it a real backend
 * counterpart, reusing the existing User entity/UserSummaryResponse the
 * way every other management endpoint in this codebase already does.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @GetMapping("/me")
    public ResponseEntity<UserSummaryResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(UserSummaryResponse.from(principal.getUser()));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserSummaryResponse> updateMe(@Valid @RequestBody UpdateProfileRequest request,
                                                          @AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> ApiException.notFound("User not found"));
        user.setFullName(request.fullName());
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        user = userRepository.save(user);
        auditLogService.log(principal.getId(), "UPDATE_OWN_PROFILE", "User", user.getId(), null);
        return ResponseEntity.ok(UserSummaryResponse.from(user));
    }
}
