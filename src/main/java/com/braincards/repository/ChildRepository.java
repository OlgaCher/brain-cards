package com.braincards.repository;

import com.braincards.model.Child;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChildRepository extends JpaRepository<Child, Long> {
    Optional<Child> findByParentId(Long parentId);
}
