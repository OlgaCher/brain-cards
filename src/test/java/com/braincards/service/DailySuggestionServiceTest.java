package com.braincards.service;

import com.braincards.model.Child;
import com.braincards.model.Game;
import com.braincards.model.Outcome;
import com.braincards.model.SessionLog;
import com.braincards.model.SwapLog;
import com.braincards.model.Zone;
import com.braincards.repository.GameRepository;
import com.braincards.repository.SessionLogRepository;
import com.braincards.repository.SwapLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailySuggestionServiceTest {

    private static final int DEFAULT_COOLDOWN = 7;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private SessionLogRepository sessionLogRepository;

    @Mock
    private SwapLogRepository swapLogRepository;

    private DailySuggestionService service;

    @BeforeEach
    void setup() {
        service = new DailySuggestionService(gameRepository, sessionLogRepository, swapLogRepository, DEFAULT_COOLDOWN);
    }

    private Zone zone(Long id) {
        Zone zone = new Zone();
        zone.setId(id);
        zone.setCode("Z" + id);
        return zone;
    }

    private Game game(Long id, Zone zone) {
        Game game = new Game();
        game.setId(id);
        game.setZone(zone);
        return game;
    }

    private Child child(Long id, LocalDate birthDate) {
        Child child = new Child();
        child.setId(id);
        child.setBirthDate(birthDate);
        return child;
    }

    private SessionLog logForGame(Game game) {
        SessionLog log = new SessionLog();
        log.setGame(game);
        log.setPlayedOn(LocalDate.now());
        log.setOutcome(Outcome.JUST_RIGHT);
        return log;
    }

    private SwapLog swapForGame(Game game) {
        SwapLog log = new SwapLog();
        log.setGame(game);
        log.setSwappedOn(LocalDate.now());
        return log;
    }

    @Test
    void suggest_returnsOneGamePerZone() {
        Zone z1 = zone(1L);
        Zone z2 = zone(2L);
        Child child = child(10L, LocalDate.now().minusYears(2));
        when(gameRepository.findCandidates(eq(10L), anyIntAge(), eq(DEFAULT_COOLDOWN)))
                .thenReturn(List.of(game(100L, z1), game(200L, z2)));
        when(sessionLogRepository.findByChildIdAndPlayedOn(10L, LocalDate.now()))
                .thenReturn(List.of());

        List<Game> result = service.suggestGamesForToday(child);

        assertThat(result).extracting(Game::getId).containsExactlyInAnyOrder(100L, 200L);
    }

    @Test
    void suggest_picksExactlyOneGameFromAZone_andIsStableAcrossCalls() {
        Zone z1 = zone(1L);
        Child child = child(10L, LocalDate.now().minusYears(2));
        when(gameRepository.findCandidates(eq(10L), anyIntAge(), eq(DEFAULT_COOLDOWN)))
                .thenReturn(List.of(game(300L, z1), game(100L, z1), game(200L, z1)));
        when(sessionLogRepository.findByChildIdAndPlayedOn(10L, LocalDate.now()))
                .thenReturn(List.of());

        List<Game> first = service.suggestGamesForToday(child);
        List<Game> second = service.suggestGamesForToday(child);

        assertThat(first).hasSize(1);
        assertThat(first.get(0).getId()).isIn(100L, 200L, 300L);
        // Stable within the day: refreshing the page must return the very same pick, not reshuffle.
        assertThat(second).extracting(Game::getId).isEqualTo(first.stream().map(Game::getId).toList());
    }

    @Test
    void suggest_excludesZoneAlreadyPlayedToday() {
        Zone z1 = zone(1L);
        Zone z2 = zone(2L);
        Child child = child(10L, LocalDate.now().minusYears(2));
        Game playedGame = game(100L, z1);
        when(gameRepository.findCandidates(eq(10L), anyIntAge(), eq(DEFAULT_COOLDOWN)))
                .thenReturn(List.of(playedGame, game(200L, z2)));
        when(sessionLogRepository.findByChildIdAndPlayedOn(10L, LocalDate.now()))
                .thenReturn(List.of(logForGame(playedGame)));

        List<Game> result = service.suggestGamesForToday(child);

        assertThat(result).extracting(Game::getId).containsExactly(200L);
    }

    @Test
    void suggest_excludesSwappedGame_andSurfacesAnotherFromSameZone() {
        Zone z1 = zone(1L);
        Child child = child(10L, LocalDate.now().minusYears(2));
        Game swapped = game(100L, z1);
        when(gameRepository.findCandidates(eq(10L), anyIntAge(), eq(DEFAULT_COOLDOWN)))
                .thenReturn(List.of(swapped, game(200L, z1)));
        when(sessionLogRepository.findByChildIdAndPlayedOn(10L, LocalDate.now()))
                .thenReturn(List.of());
        when(swapLogRepository.findByChildIdAndSwappedOn(10L, LocalDate.now()))
                .thenReturn(List.of(swapForGame(swapped)));

        List<Game> result = service.suggestGamesForToday(child);

        assertThat(result).extracting(Game::getId).containsExactly(200L);
    }

    @Test
    void suggest_dropsZone_whenSwappedGameWasTheLastInThatZone() {
        Zone z1 = zone(1L);
        Child child = child(10L, LocalDate.now().minusYears(2));
        Game onlyGame = game(100L, z1);
        when(gameRepository.findCandidates(eq(10L), anyIntAge(), eq(DEFAULT_COOLDOWN)))
                .thenReturn(List.of(onlyGame));
        when(sessionLogRepository.findByChildIdAndPlayedOn(10L, LocalDate.now()))
                .thenReturn(List.of());
        when(swapLogRepository.findByChildIdAndSwappedOn(10L, LocalDate.now()))
                .thenReturn(List.of(swapForGame(onlyGame)));

        assertThat(service.suggestGamesForToday(child)).isEmpty();
    }

    @Test
    void suggest_returnsEmpty_whenNoCandidates() {
        Child child = child(10L, LocalDate.now().minusYears(2));
        when(gameRepository.findCandidates(eq(10L), anyIntAge(), eq(DEFAULT_COOLDOWN)))
                .thenReturn(List.of());
        when(sessionLogRepository.findByChildIdAndPlayedOn(10L, LocalDate.now()))
                .thenReturn(List.of());

        assertThat(service.suggestGamesForToday(child)).isEmpty();
    }

    @Test
    void suggest_treatsNullBirthDateAsAgeZero() {
        Child child = child(10L, null);
        when(gameRepository.findCandidates(eq(10L), eq(0), eq(DEFAULT_COOLDOWN)))
                .thenReturn(List.of());
        when(sessionLogRepository.findByChildIdAndPlayedOn(10L, LocalDate.now()))
                .thenReturn(List.of());

        service.suggestGamesForToday(child);

        ArgumentCaptor<Integer> ageCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(gameRepository).findCandidates(eq(10L), ageCaptor.capture(), eq(DEFAULT_COOLDOWN));
        assertThat(ageCaptor.getValue()).isZero();
    }

    @Test
    void suggest_passesComputedAgeInMonths() {
        LocalDate birth = LocalDate.now().minusMonths(30);
        int expectedMonths = (int) Period.between(birth, LocalDate.now()).toTotalMonths();
        Child child = child(10L, birth);
        when(gameRepository.findCandidates(eq(10L), eq(expectedMonths), eq(DEFAULT_COOLDOWN)))
                .thenReturn(List.of());
        when(sessionLogRepository.findByChildIdAndPlayedOn(10L, LocalDate.now()))
                .thenReturn(List.of());

        service.suggestGamesForToday(child);

        verify(gameRepository).findCandidates(eq(10L), eq(expectedMonths), eq(DEFAULT_COOLDOWN));
    }

    @Test
    void recordSwap_savesSwapLogForTodayWithChildAndGame() {
        Zone z1 = zone(1L);
        Child child = child(20L, LocalDate.now().minusYears(2));
        Game target = game(100L, z1);
        when(swapLogRepository.existsByChildIdAndGameIdAndSwappedOn(20L, 100L, LocalDate.now()))
                .thenReturn(false);
        when(gameRepository.findById(100L)).thenReturn(Optional.of(target));

        service.recordSwap(child, 100L);

        ArgumentCaptor<SwapLog> captor = ArgumentCaptor.forClass(SwapLog.class);
        verify(swapLogRepository).save(captor.capture());
        SwapLog saved = captor.getValue();
        assertThat(saved.getChild()).isSameAs(child);
        assertThat(saved.getGame()).isSameAs(target);
        assertThat(saved.getSwappedOn()).isEqualTo(LocalDate.now());
    }

    @Test
    void recordSwap_isIdempotent_whenAlreadySwappedToday() {
        Child child = child(20L, LocalDate.now().minusYears(2));
        when(swapLogRepository.existsByChildIdAndGameIdAndSwappedOn(20L, 100L, LocalDate.now()))
                .thenReturn(true);

        service.recordSwap(child, 100L);

        verify(gameRepository, never()).findById(anyLong());
        verify(swapLogRepository, never()).save(any());
    }

    @Test
    void recordSwap_throwsWhenGameDoesNotExist() {
        Child child = child(20L, LocalDate.now().minusYears(2));
        when(swapLogRepository.existsByChildIdAndGameIdAndSwappedOn(20L, 999L, LocalDate.now()))
                .thenReturn(false);
        when(gameRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.recordSwap(child, 999L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(swapLogRepository, never()).save(any());
    }

    private int anyIntAge() {
        return org.mockito.ArgumentMatchers.anyInt();
    }
}
