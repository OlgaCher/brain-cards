package com.braincards.repository;

import com.braincards.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GameRepository extends JpaRepository<Game, Long> {

    List<Game> findByZoneId(Long zoneId);

    long countByZoneIdAndActiveTrue(Long zoneId);

    @Query(value = """
            SELECT g.* FROM game g
            WHERE g.active = true
              AND :ageMonths BETWEEN g.min_age_months AND g.max_age_months
              AND NOT EXISTS (
                  SELECT 1 FROM session_log sl
                  WHERE sl.game_id = g.id
                    AND sl.child_id = :childId
                    AND sl.played_on > (CURRENT_DATE - COALESCE(g.cooldown_days, :defaultCooldownDays))
              )
            """, nativeQuery = true)
    List<Game> findCandidates(@Param("childId") Long childId,
                               @Param("ageMonths") int ageMonths,
                               @Param("defaultCooldownDays") int defaultCooldownDays);
}
