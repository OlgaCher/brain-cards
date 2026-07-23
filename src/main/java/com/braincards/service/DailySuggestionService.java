package com.braincards.service;

import com.braincards.model.Child;
import com.braincards.model.Game;
import com.braincards.model.SwapLog;
import com.braincards.repository.GameRepository;
import com.braincards.repository.SessionLogRepository;
import com.braincards.repository.SwapLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class DailySuggestionService {

    private final GameRepository gameRepository;
    private final SessionLogRepository sessionLogRepository;
    private final SwapLogRepository swapLogRepository;
    private final int defaultCooldownDays;

    public DailySuggestionService(GameRepository gameRepository,
                                   SessionLogRepository sessionLogRepository,
                                   SwapLogRepository swapLogRepository,
                                   @Value("${braincards.suggestion.cooldown-days}") int defaultCooldownDays) {
        this.gameRepository = gameRepository;
        this.sessionLogRepository = sessionLogRepository;
        this.swapLogRepository = swapLogRepository;
        this.defaultCooldownDays = defaultCooldownDays;
    }

    public List<Game> suggestGamesForToday(Child child) {
        int ageMonths = ageInMonths(child.getBirthDate());
        List<Game> candidates = gameRepository.findCandidates(child.getId(), ageMonths, defaultCooldownDays);
        Set<Long> zonesDoneToday = zonesPlayedToday(child.getId());
        Set<Long> gamesSwappedToday = gamesSwappedToday(child.getId());

        Map<Long, List<Game>> byZone = new TreeMap<>();
        for (Game candidate : candidates) {
            Long zoneId = candidate.getZone().getId();
            if (zonesDoneToday.contains(zoneId) || gamesSwappedToday.contains(candidate.getId())) {
                continue;
            }
            byZone.computeIfAbsent(zoneId, key -> new ArrayList<>()).add(candidate);
        }

        List<Game> result = new ArrayList<>();
        for (Map.Entry<Long, List<Game>> entry : byZone.entrySet()) {
            result.add(pickForZone(child.getId(), entry.getKey(), entry.getValue()));
        }
        return result;
    }

    @Transactional
    public void recordSwap(Child child, Long gameId) {
        if (swapLogRepository.existsByChildIdAndGameIdAndSwappedOn(child.getId(), gameId, LocalDate.now())) {
            return;
        }
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Game not found: " + gameId));
        SwapLog swapLog = new SwapLog();
        swapLog.setChild(child);
        swapLog.setGame(game);
        swapLog.setSwappedOn(LocalDate.now());
        swapLogRepository.save(swapLog);
    }

    private Game pickForZone(Long childId, Long zoneId, List<Game> zoneCandidates) {
        List<Game> sorted = zoneCandidates.stream()
                .sorted(Comparator.comparing(Game::getId))
                .toList();
        int seed = Objects.hash(childId, zoneId, LocalDate.now().toEpochDay());
        int index = Math.floorMod(seed, sorted.size());
        return sorted.get(index);
    }

    private Set<Long> zonesPlayedToday(Long childId) {
        return sessionLogRepository.findByChildIdAndPlayedOn(childId, LocalDate.now()).stream()
                .map(sessionLog -> sessionLog.getGame().getZone().getId())
                .collect(Collectors.toSet());
    }

    private Set<Long> gamesSwappedToday(Long childId) {
        return swapLogRepository.findByChildIdAndSwappedOn(childId, LocalDate.now()).stream()
                .map(swapLog -> swapLog.getGame().getId())
                .collect(Collectors.toSet());
    }

    private int ageInMonths(LocalDate birthDate) {
        if (birthDate == null) {
            return 0;
        }
        return (int) Period.between(birthDate, LocalDate.now()).toTotalMonths();
    }
}
