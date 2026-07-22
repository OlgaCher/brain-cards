package com.braincards.service;

import com.braincards.dto.ParentDto;
import com.braincards.model.Parent;
import com.braincards.repository.ParentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParentServiceTest {

    @Mock
    private ParentRepository parentRepository;

    @InjectMocks
    private ParentService parentService;

    @Test
    void getProfile_mapsAllFields() {
        Parent parent = new Parent();
        parent.setId(5L);
        parent.setEmail("p@test.com");
        parent.setDisplayName("Olha");
        parent.setLocale("UA");
        when(parentRepository.findById(5L)).thenReturn(Optional.of(parent));

        ParentDto dto = parentService.getProfile(5L);

        assertThat(dto.id()).isEqualTo(5L);
        assertThat(dto.email()).isEqualTo("p@test.com");
        assertThat(dto.displayName()).isEqualTo("Olha");
        assertThat(dto.locale()).isEqualTo("UA");
    }

    @Test
    void getProfile_throwsNotFound_whenMissing() {
        when(parentRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> parentService.getProfile(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Parent not found: 404");
    }
}
