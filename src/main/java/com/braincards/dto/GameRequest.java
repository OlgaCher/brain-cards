package com.braincards.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record GameRequest(
        @NotNull Long zoneId,
        @NotNull Integer minAgeMonths,
        @NotNull Integer maxAgeMonths,
        Boolean active,
        Integer cooldownDays,
        @NotEmpty @Valid List<GameTranslationRequest> translations
) {
}
