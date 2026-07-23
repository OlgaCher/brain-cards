package com.braincards.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CoachRequest(
        @NotNull Long gameId,
        @NotBlank @Size(max = 1000) String question
) {
}
