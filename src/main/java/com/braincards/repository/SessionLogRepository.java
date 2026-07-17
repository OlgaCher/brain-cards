package com.braincards.repository;

import com.braincards.model.SessionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SessionLogRepository extends JpaRepository<SessionLog, Long> {
    List<SessionLog> findByChildId(Long childId);
    List<SessionLog> findByChildIdAndPlayedOn(Long childId, LocalDate playedOn);
}
