package com.braincards.dto;

import java.time.LocalDateTime;

public record ParentDto(Long id, String email, String displayName, String locale, LocalDateTime createdAt) {
}
