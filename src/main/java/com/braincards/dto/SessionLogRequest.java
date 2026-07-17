package com.braincards.dto;

import com.braincards.model.Outcome;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SessionLogRequest(@NotNull Long gameId, @NotNull LocalDate playedOn, @NotNull Outcome outcome,
                                 Integer durationMin, String parentNote) {
}
