package com.rrtechnosoft.lms.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCompanyRequest(
        @NotBlank @Size(min = 2, max = 200) String name,
        String logoUrl,
        String website,
        @Size(max = 150) String industry,
        String description,
        @Size(max = 150) String contactPersonName,
        @Email @Size(max = 150) String contactEmail,
        @Size(max = 30) String contactPhone,
        @Size(max = 300) String address
) {}
