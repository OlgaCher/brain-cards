package com.braincards.service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class ParentUserDetails implements UserDetails {

    private final Long parentId;
    private final String email;
    private final String passwordHash;

    public ParentUserDetails(Long parentId, String email, String passwordHash) {
        this.parentId = parentId;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public Long getParentId() {
        return parentId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_PARENT"));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
