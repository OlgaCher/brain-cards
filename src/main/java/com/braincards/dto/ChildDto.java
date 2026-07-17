package com.braincards.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ChildDto(Long id, String name, LocalDate birthDate, LocalDateTime createdAt) {
}
