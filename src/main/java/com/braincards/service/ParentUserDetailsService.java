package com.braincards.service;

import com.braincards.model.Parent;
import com.braincards.repository.ParentRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ParentUserDetailsService implements UserDetailsService {

    private final ParentRepository parentRepository;

    public ParentUserDetailsService(ParentRepository parentRepository) {
        this.parentRepository = parentRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        Parent parent = parentRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No parent found for email: " + email));
        return new ParentUserDetails(parent.getId(), parent.getEmail(), parent.getPasswordHash());
    }
}
