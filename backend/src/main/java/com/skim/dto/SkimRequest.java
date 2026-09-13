package com.skim.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SkimRequest {

    @NotBlank(message = "Content must not be empty")
    @Size(
            min = 10,
            max = 5000,
            message = "Content must be between 10 and 1000 characters"
    )
    private String content;

    @NotBlank(message = "Operation must not be empty")
    private String operation;

    // Only required when operation = "rewrite"; validated manually in SkimService
    private String tone;
}
