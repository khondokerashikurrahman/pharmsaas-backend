package com.pharmsaas.backend.modules.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductCategoryRequest(
    @NotBlank(message = "Product type is required")
    @Size(max = 30)
    String productType,

    @NotBlank(message = "Category name is required")
    @Size(max = 100)
    String name
) {}
