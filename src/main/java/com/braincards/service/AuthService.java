package com.braincards.service;

import com.braincards.dto.RegistrationForm;
import com.braincards.model.Child;
import com.braincards.model.Parent;
import com.braincards.repository.ParentRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final ParentRepository parentRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(ParentRepository parentRepository, PasswordEncoder passwordEncoder) {
        this.parentRepository = parentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Parent register(RegistrationForm form) {
        Parent parent = new Parent();
        parent.setEmail(form.getEmail());
        parent.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        parent.setDisplayName(form.getDisplayName());

        Child child = new Child();
        child.setName(form.getChildName());
        child.setBirthDate(form.getChildBirthDate());
        child.setParent(parent);
        parent.setChild(child);

        return parentRepository.save(parent);
    }
}
