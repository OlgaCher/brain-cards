package com.braincards.repository;

import com.braincards.model.DailyPick;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyPickRepository extends JpaRepository<DailyPick, Long> {
    List<DailyPick> findByChildIdAndPickDate(Long childId, LocalDate pickDate);
    Optional<DailyPick> findByChildIdAndZoneIdAndPickDate(Long childId, Long zoneId, LocalDate pickDate);
}
