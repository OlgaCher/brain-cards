package com.braincards.dto;

import com.braincards.model.Outcome;

import java.time.LocalDate;

public record SessionLogDto(Long id, Long childId, Long gameId, LocalDate playedOn,
                             Outcome outcome, Integer durationMin, String parentNote) {
}
