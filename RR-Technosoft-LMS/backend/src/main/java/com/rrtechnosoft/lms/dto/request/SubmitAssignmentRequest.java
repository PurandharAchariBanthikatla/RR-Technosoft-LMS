package com.rrtechnosoft.lms.dto.request;

public record SubmitAssignmentRequest(
        String fileUrl,
        String text
) {}
