package com.braincards.dto;

import jakarta.validation.constraints.NotBlank;

public record GameTranslationRequest(@NotBlank String locale, @NotBlank String title, String instructions) {
}
