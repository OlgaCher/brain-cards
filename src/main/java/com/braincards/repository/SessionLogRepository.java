package com.braincards.repository;

import com.braincards.model.SessionLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionLogRepository extends JpaRepository<SessionLog, Long> {
}
