package com.braincards.dto;

public record GameDto(Long id, Long zoneId, String zoneName, Integer minAgeMonths, Integer maxAgeMonths,
                       boolean active, Integer cooldownDays, String title, String instructions) {
}
