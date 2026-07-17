package com.braincards.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record ChildRequest(@NotBlank String name, LocalDate birthDate) {
}
