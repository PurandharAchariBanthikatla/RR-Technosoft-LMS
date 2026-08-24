package com.rrtechnosoft.lms.dto.request;

/** Body is optional — a student can apply with just a resume URL, or with nothing at all. */
public record ApplyPlacementRequest(String resumeUrl) {}
