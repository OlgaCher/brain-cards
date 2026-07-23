package com.braincards.repository;

import com.braincards.model.SwapLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SwapLogRepository extends JpaRepository<SwapLog, Long> {
    List<SwapLog> findByChildIdAndSwappedOn(Long childId, LocalDate swappedOn);
    boolean existsByChildIdAndGameIdAndSwappedOn(Long childId, Long gameId, LocalDate swappedOn);
}
