package com.braincards.service;

import com.braincards.dto.ChildDto;
import com.braincards.dto.ChildRequest;
import com.braincards.model.Child;
import com.braincards.model.Parent;
import com.braincards.repository.ChildRepository;
import com.braincards.repository.ParentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChildServiceTest {

    @Mock
    private ChildRepository childRepository;

    @Mock
    private ParentRepository parentRepository;

    @InjectMocks
    private ChildService childService;

    private static final Long PARENT_ID = 1L;

    private Child sampleChild() {
        Child child = new Child();
        child.setId(10L);
        child.setName("Sofia");
        child.setBirthDate(LocalDate.of(2022, 1, 1));
        return child;
    }

    @Test
    void getMyChild_returnsDto_whenChildExists() {
        when(childRepository.findByParentId(PARENT_ID)).thenReturn(Optional.of(sampleChild()));

        ChildDto dto = childService.getMyChild(PARENT_ID);

        assertThat(dto.id()).isEqualTo(10L);
        assertThat(dto.name()).isEqualTo("Sofia");
        assertThat(dto.birthDate()).isEqualTo(LocalDate.of(2022, 1, 1));
    }

    @Test
    void getMyChild_throwsNotFound_whenNoChild() {
        when(childRepository.findByParentId(PARENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> childService.getMyChild(PARENT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No child found");
    }

    @Test
    void createChild_persistsAndReturnsDto_whenNoExistingChild() {
        ChildRequest request = new ChildRequest("Max", LocalDate.of(2021, 5, 20));
        Parent parent = new Parent();
        parent.setId(PARENT_ID);
        when(childRepository.findByParentId(PARENT_ID)).thenReturn(Optional.empty());
        when(parentRepository.findById(PARENT_ID)).thenReturn(Optional.of(parent));
        when(childRepository.save(any(Child.class))).thenAnswer(inv -> {
            Child c = inv.getArgument(0);
            c.setId(99L);
            return c;
        });

        ChildDto dto = childService.createChild(PARENT_ID, request);

        ArgumentCaptor<Child> captor = ArgumentCaptor.forClass(Child.class);
        verify(childRepository).save(captor.capture());
        Child saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Max");
        assertThat(saved.getBirthDate()).isEqualTo(LocalDate.of(2021, 5, 20));
        assertThat(saved.getParent()).isSameAs(parent);
        assertThat(dto.id()).isEqualTo(99L);
        assertThat(dto.name()).isEqualTo("Max");
    }

    @Test
    void createChild_throwsIllegalArgument_whenParentAlreadyHasChild() {
        ChildRequest request = new ChildRequest("Max", LocalDate.of(2021, 5, 20));
        when(childRepository.findByParentId(PARENT_ID)).thenReturn(Optional.of(sampleChild()));

        assertThatThrownBy(() -> childService.createChild(PARENT_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already has a child");

        verify(childRepository, never()).save(any());
    }

    @Test
    void createChild_throwsNotFound_whenParentMissing() {
        ChildRequest request = new ChildRequest("Max", LocalDate.of(2021, 5, 20));
        when(childRepository.findByParentId(PARENT_ID)).thenReturn(Optional.empty());
        when(parentRepository.findById(PARENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> childService.createChild(PARENT_ID, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Parent not found");

        verify(childRepository, never()).save(any());
    }

    @Test
    void createChild_allowsNullBirthDate_edgeCase() {
        ChildRequest request = new ChildRequest("NoBirthday", null);
        Parent parent = new Parent();
        parent.setId(PARENT_ID);
        when(childRepository.findByParentId(PARENT_ID)).thenReturn(Optional.empty());
        when(parentRepository.findById(PARENT_ID)).thenReturn(Optional.of(parent));
        when(childRepository.save(any(Child.class))).thenAnswer(inv -> inv.getArgument(0));

        ChildDto dto = childService.createChild(PARENT_ID, request);

        assertThat(dto.name()).isEqualTo("NoBirthday");
        assertThat(dto.birthDate()).isNull();
    }

    @Test
    void updateChild_mutatesExistingChild() {
        Child existing = sampleChild();
        when(childRepository.findByParentId(PARENT_ID)).thenReturn(Optional.of(existing));
        ChildRequest request = new ChildRequest("Renamed", LocalDate.of(2020, 3, 3));

        ChildDto dto = childService.updateChild(PARENT_ID, request);

        assertThat(existing.getName()).isEqualTo("Renamed");
        assertThat(existing.getBirthDate()).isEqualTo(LocalDate.of(2020, 3, 3));
        assertThat(dto.name()).isEqualTo("Renamed");
        verify(childRepository, never()).save(any());
    }

    @Test
    void updateChild_throwsNotFound_whenNoChild() {
        when(childRepository.findByParentId(PARENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> childService.updateChild(PARENT_ID, new ChildRequest("X", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteChild_deletesResolvedEntity() {
        Child existing = sampleChild();
        when(childRepository.findByParentId(PARENT_ID)).thenReturn(Optional.of(existing));

        childService.deleteChild(PARENT_ID);

        verify(childRepository).delete(existing);
    }

    @Test
    void deleteChild_throwsNotFound_whenNoChild() {
        when(childRepository.findByParentId(PARENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> childService.deleteChild(PARENT_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(childRepository, never()).delete(any());
    }
}
