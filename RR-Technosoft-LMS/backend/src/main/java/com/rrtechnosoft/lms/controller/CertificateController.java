package com.rrtechnosoft.lms.controller;

import com.rrtechnosoft.lms.dto.request.IssueCertificateRequest;
import com.rrtechnosoft.lms.dto.response.CertificateResponse;
import com.rrtechnosoft.lms.security.UserPrincipal;
import com.rrtechnosoft.lms.service.CertificateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * /verify/{code} is intentionally NOT behind @PreAuthorize — it's the public
 * endpoint a QR code scan (or the public frontend /verify/[code] page) hits,
 * so it must work for an unauthenticated third party checking a certificate's
 * authenticity. See SecurityConfig for the matching permit-all rule.
 */
@RestController
@RequestMapping("/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;

    @GetMapping("/me")
    public ResponseEntity<List<CertificateResponse>> mine(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(certificateService.mine(principal.getId()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Page<CertificateResponse>> list(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(certificateService.list(pageable));
    }

    @GetMapping("/verify/{code}")
    public ResponseEntity<CertificateResponse> verify(@PathVariable String code) {
        return ResponseEntity.ok(certificateService.verify(code));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<CertificateResponse> issue(@Valid @RequestBody IssueCertificateRequest request,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        CertificateResponse issued = certificateService.issue(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(issued);
    }
}
