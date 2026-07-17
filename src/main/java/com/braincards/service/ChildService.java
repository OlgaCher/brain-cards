package com.braincards.service;

import com.braincards.dto.ChildDto;
import com.braincards.dto.ChildRequest;
import com.braincards.model.Child;
import com.braincards.model.Parent;
import com.braincards.repository.ChildRepository;
import com.braincards.repository.ParentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChildService {

    private final ChildRepository childRepository;
    private final ParentRepository parentRepository;

    public ChildService(ChildRepository childRepository, ParentRepository parentRepository) {
        this.childRepository = childRepository;
        this.parentRepository = parentRepository;
    }

    public ChildDto getMyChild(Long parentId) {
        return toDto(findChildEntity(parentId));
    }

    // Resolves "my child" strictly through the authenticated parent's own id -
    // callers never pass a client-supplied child id, so this is the ownership boundary.
    public Child findChildEntity(Long parentId) {
        return childRepository.findByParentId(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("No child found for this parent"));
    }

    @Transactional
    public ChildDto createChild(Long parentId, ChildRequest request) {
        if (childRepository.findByParentId(parentId).isPresent()) {
            throw new IllegalArgumentException("Parent already has a child");
        }
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent not found: " + parentId));

        Child child = new Child();
        child.setParent(parent);
        child.setName(request.name());
        child.setBirthDate(request.birthDate());
        return toDto(childRepository.save(child));
    }

    @Transactional
    public ChildDto updateChild(Long parentId, ChildRequest request) {
        Child child = findChildEntity(parentId);
        child.setName(request.name());
        child.setBirthDate(request.birthDate());
        return toDto(child);
    }

    @Transactional
    public void deleteChild(Long parentId) {
        childRepository.delete(findChildEntity(parentId));
    }

    private ChildDto toDto(Child child) {
        return new ChildDto(child.getId(), child.getName(), child.getBirthDate(), child.getCreatedAt());
    }
}
