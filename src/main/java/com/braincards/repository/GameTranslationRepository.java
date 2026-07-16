package com.braincards.repository;

import com.braincards.model.GameTranslation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameTranslationRepository extends JpaRepository<GameTranslation, Long> {
}
