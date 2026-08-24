package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.Certificate;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Field names pinned to the frontend `Certificate` type in src/types/index.ts. */
public record CertificateResponse(
        UUID id,
        String courseTitle,
        String studentName,
        OffsetDateTime issuedAt,
        String certificateUrl,
        String verificationCode
) {
    public static CertificateResponse of(Certificate c, String certificateUrl) {
        return new CertificateResponse(
                c.getId(),
                c.getCourse().getTitle(),
                c.getStudent().getFullName(),
                c.getIssuedAt(),
                certificateUrl,
                c.getCertificateNo()
        );
    }
}
