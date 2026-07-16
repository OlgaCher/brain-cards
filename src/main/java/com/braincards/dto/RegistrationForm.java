package com.braincards.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class RegistrationForm {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    private String displayName;

    @NotBlank
    private String childName;

    private LocalDate childBirthDate;
}
