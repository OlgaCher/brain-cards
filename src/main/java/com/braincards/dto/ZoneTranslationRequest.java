package com.braincards.dto;

import jakarta.validation.constraints.NotBlank;

public record ZoneTranslationRequest(@NotBlank String locale, @NotBlank String name) {
}
