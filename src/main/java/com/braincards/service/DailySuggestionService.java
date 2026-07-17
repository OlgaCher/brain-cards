package com.braincards.service;

import com.braincards.model.Child;
import com.braincards.model.Game;
import com.braincards.repository.GameRepository;
import com.braincards.repository.SessionLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class DailySuggestionService {

    private final GameRepository gameRepository;
    private final SessionLogRepository sessionLogRepository;
    private final int defaultCooldownDays;

    public DailySuggestionService(GameRepository gameRepository, SessionLogRepository sessionLogRepository,
                                   @Value("${braincards.suggestion.cooldown-days}") int defaultCooldownDays) {
        this.gameRepository = gameRepository;
        this.sessionLogRepository = sessionLogRepository;
        this.defaultCooldownDays = defaultCooldownDays;
    }

    // TODO: outcome-aware cooldowns are a planned future refinement - e.g. a game marked EASY
    // could become suggestible again sooner than one marked TOO_HARD. Today every game uses the
    // same fixed cooldown window (braincards.suggestion.cooldown-days, or the game's own override)
    // regardless of how the last session went.
    //
    // One game per zone, picking the lowest-id eligible candidate within each zone so repeated
    // calls on the same day return the same set (no swap feature yet - a random pick would look
    // like an accidental swap on every page refresh). A zone with no eligible candidate today
    // (age range or cooldown) is simply absent, so this can return fewer than "one per zone".
    //
    // A zone whose game was already marked done today is also left out entirely, rather than
    // substituting a different game from the same zone - "done" means that zone's slot for today
    // is used up, not "pick me a different one from here".
    public List<Game> suggestGamesForToday(Child child) {
        int ageMonths = ageInMonths(child.getBirthDate());
        List<Game> candidates = gameRepository.findCandidates(child.getId(), ageMonths, defaultCooldownDays);
        Set<Long> zonesDoneToday = zonesPlayedToday(child.getId());

        Map<Long, Game> pickPerZone = new TreeMap<>();
        for (Game candidate : candidates) {
            Long zoneId = candidate.getZone().getId();
            if (zonesDoneToday.contains(zoneId)) {
                continue;
            }
            pickPerZone.merge(zoneId, candidate,
                    (existing, incoming) -> incoming.getId() < existing.getId() ? incoming : existing);
        }
        return new ArrayList<>(pickPerZone.values());
    }

    private Set<Long> zonesPlayedToday(Long childId) {
        return sessionLogRepository.findByChildIdAndPlayedOn(childId, LocalDate.now()).stream()
                .map(sessionLog -> sessionLog.getGame().getZone().getId())
                .collect(Collectors.toSet());
    }

    private int ageInMonths(LocalDate birthDate) {
        if (birthDate == null) {
            return 0;
        }
        return (int) Period.between(birthDate, LocalDate.now()).toTotalMonths();
    }
}
