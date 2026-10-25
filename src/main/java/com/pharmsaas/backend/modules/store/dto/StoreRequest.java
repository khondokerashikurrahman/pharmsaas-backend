package com.pharmsaas.backend.modules.store.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StoreRequest(

        @NotBlank(message = "Store code is required")
        @Size(max = 30, message = "Store code cannot exceed 30 characters")
        String storeCode,

        @NotBlank(message = "Store name is required")
        @Size(max = 150, message = "Store name cannot exceed 150 characters")
        String name,

        @Size(max = 30)
        String phone,

        @Email(message = "Invalid email address")
        @Size(max = 255)
        String email,

        @Size(max = 500)
        String address,

        @Size(max = 100)
        String city,

        @Size(max = 100)
        String district,

        String status

) {
}
