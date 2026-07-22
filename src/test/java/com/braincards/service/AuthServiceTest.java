package com.braincards.service;

import com.braincards.dto.RegistrationForm;
import com.braincards.model.Parent;
import com.braincards.repository.ParentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private ParentRepository parentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private RegistrationForm form(String email, String password, String childName, LocalDate childBirth) {
        RegistrationForm form = new RegistrationForm();
        form.setEmail(email);
        form.setPassword(password);
        form.setDisplayName("Matilda");
        form.setChildName(childName);
        form.setChildBirthDate(childBirth);
        return form;
    }

    @Test
    void register_hashesPasswordAndLinksChildBidirectionally() {
        RegistrationForm form = form("parent@test.com", "secret", "Sofia", LocalDate.of(2022, 1, 1));
        when(passwordEncoder.encode("secret")).thenReturn("ENCODED");
        when(parentRepository.save(any(Parent.class))).thenAnswer(inv -> inv.getArgument(0));

        Parent result = authService.register(form);

        ArgumentCaptor<Parent> captor = ArgumentCaptor.forClass(Parent.class);
        verify(parentRepository).save(captor.capture());
        Parent saved = captor.getValue();

        assertThat(saved.getEmail()).isEqualTo("parent@test.com");
        assertThat(saved.getPasswordHash()).isEqualTo("ENCODED");
        assertThat(saved.getDisplayName()).isEqualTo("Matilda");
        assertThat(saved.getChild()).isNotNull();
        assertThat(saved.getChild().getName()).isEqualTo("Sofia");
        assertThat(saved.getChild().getBirthDate()).isEqualTo(LocalDate.of(2022, 1, 1));
        assertThat(saved.getChild().getParent()).isSameAs(saved);
        assertThat(result).isSameAs(saved);
    }

    @Test
    void register_allowsNullChildBirthDate_edgeCase() {
        RegistrationForm form = form("a@b.com", "pw", "Kid", null);
        when(passwordEncoder.encode(any())).thenReturn("h");
        when(parentRepository.save(any(Parent.class))).thenAnswer(inv -> inv.getArgument(0));

        Parent result = authService.register(form);

        assertThat(result.getChild().getBirthDate()).isNull();
    }
}
