package com.braincards.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ZoneRequest(@NotBlank String code, @NotEmpty @Valid List<ZoneTranslationRequest> translations) {
}
