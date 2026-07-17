package com.braincards.service;

import com.braincards.dto.ParentDto;
import com.braincards.model.Parent;
import com.braincards.repository.ParentRepository;
import org.springframework.stereotype.Service;

@Service
public class ParentService {

    private final ParentRepository parentRepository;

    public ParentService(ParentRepository parentRepository) {
        this.parentRepository = parentRepository;
    }

    public ParentDto getProfile(Long parentId) {
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent not found: " + parentId));
        return toDto(parent);
    }

    private ParentDto toDto(Parent parent) {
        return new ParentDto(parent.getId(), parent.getEmail(), parent.getDisplayName(), parent.getLocale(), parent.getCreatedAt());
    }
}
