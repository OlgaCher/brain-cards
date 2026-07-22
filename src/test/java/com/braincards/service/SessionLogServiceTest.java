package com.braincards.service;

import com.braincards.dto.SessionLogDto;
import com.braincards.dto.SessionLogRequest;
import com.braincards.model.Child;
import com.braincards.model.Game;
import com.braincards.model.Outcome;
import com.braincards.model.Parent;
import com.braincards.model.SessionLog;
import com.braincards.repository.GameRepository;
import com.braincards.repository.SessionLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionLogServiceTest {

    @Mock
    private SessionLogRepository sessionLogRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private ChildService childService;

    @InjectMocks
    private SessionLogService sessionLogService;

    private static final Long PARENT_ID = 1L;

    private Child childOwnedBy(Long parentId, Long childId) {
        Parent parent = new Parent();
        parent.setId(parentId);
        Child child = new Child();
        child.setId(childId);
        child.setParent(parent);
        return child;
    }

    private Game game(Long id) {
        Game g = new Game();
        g.setId(id);
        return g;
    }

    private SessionLog log(Long id, Child child, Game game) {
        SessionLog log = new SessionLog();
        log.setId(id);
        log.setChild(child);
        log.setGame(game);
        log.setPlayedOn(LocalDate.of(2026, 7, 1));
        log.setOutcome(Outcome.JUST_RIGHT);
        log.setDurationMin(15);
        log.setParentNote("note");
        return log;
    }

    @Test
    void listMine_returnsAllLogs_whenDateNull() {
        Child child = childOwnedBy(PARENT_ID, 10L);
        when(childService.findChildEntity(PARENT_ID)).thenReturn(child);
        when(sessionLogRepository.findByChildId(10L))
                .thenReturn(List.of(log(1L, child, game(2L))));

        List<SessionLogDto> result = sessionLogService.listMine(PARENT_ID, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).childId()).isEqualTo(10L);
        verify(sessionLogRepository).findByChildId(10L);
        verify(sessionLogRepository, never()).findByChildIdAndPlayedOn(any(), any());
    }

    @Test
    void listMine_filtersByDate_whenDateProvided() {
        Child child = childOwnedBy(PARENT_ID, 10L);
        LocalDate date = LocalDate.of(2026, 7, 1);
        when(childService.findChildEntity(PARENT_ID)).thenReturn(child);
        when(sessionLogRepository.findByChildIdAndPlayedOn(10L, date))
                .thenReturn(List.of(log(1L, child, game(2L))));

        List<SessionLogDto> result = sessionLogService.listMine(PARENT_ID, date);

        assertThat(result).hasSize(1);
        verify(sessionLogRepository).findByChildIdAndPlayedOn(10L, date);
    }

    @Test
    void listMine_returnsEmpty_whenNoLogs_edgeCase() {
        Child child = childOwnedBy(PARENT_ID, 10L);
        when(childService.findChildEntity(PARENT_ID)).thenReturn(child);
        when(sessionLogRepository.findByChildId(10L)).thenReturn(List.of());

        assertThat(sessionLogService.listMine(PARENT_ID, null)).isEmpty();
    }

    @Test
    void create_persistsLogForOwnChildAndGame() {
        Child child = childOwnedBy(PARENT_ID, 10L);
        Game game = game(2L);
        SessionLogRequest request = new SessionLogRequest(2L, LocalDate.of(2026, 7, 5),
                Outcome.EASY, 20, "great");
        when(childService.findChildEntity(PARENT_ID)).thenReturn(child);
        when(gameRepository.findById(2L)).thenReturn(Optional.of(game));
        when(sessionLogRepository.save(any(SessionLog.class))).thenAnswer(inv -> {
            SessionLog l = inv.getArgument(0);
            l.setId(500L);
            return l;
        });

        SessionLogDto dto = sessionLogService.create(PARENT_ID, request);

        ArgumentCaptor<SessionLog> captor = ArgumentCaptor.forClass(SessionLog.class);
        verify(sessionLogRepository).save(captor.capture());
        SessionLog saved = captor.getValue();
        assertThat(saved.getChild()).isSameAs(child);
        assertThat(saved.getGame()).isSameAs(game);
        assertThat(saved.getOutcome()).isEqualTo(Outcome.EASY);
        assertThat(dto.id()).isEqualTo(500L);
        assertThat(dto.gameId()).isEqualTo(2L);
    }

    @Test
    void create_throwsNotFound_whenGameMissing() {
        Child child = childOwnedBy(PARENT_ID, 10L);
        SessionLogRequest request = new SessionLogRequest(99L, LocalDate.now(), Outcome.EASY, 1, null);
        when(childService.findChildEntity(PARENT_ID)).thenReturn(child);
        when(gameRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionLogService.create(PARENT_ID, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Game not found: 99");

        verify(sessionLogRepository, never()).save(any());
    }

    @Test
    void update_mutatesOwnedLog() {
        Child child = childOwnedBy(PARENT_ID, 10L);
        SessionLog existing = log(1L, child, game(2L));
        Game newGame = game(3L);
        when(sessionLogRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(gameRepository.findById(3L)).thenReturn(Optional.of(newGame));
        SessionLogRequest request = new SessionLogRequest(3L, LocalDate.of(2026, 8, 1),
                Outcome.TOO_HARD, 30, "hard");

        SessionLogDto dto = sessionLogService.update(PARENT_ID, 1L, request);

        assertThat(existing.getGame()).isSameAs(newGame);
        assertThat(existing.getOutcome()).isEqualTo(Outcome.TOO_HARD);
        assertThat(dto.durationMin()).isEqualTo(30);
        verify(sessionLogRepository, never()).save(any());
    }

    @Test
    void update_throwsAccessDenied_whenLogBelongsToAnotherParent() {
        Child otherChild = childOwnedBy(999L, 77L);
        SessionLog existing = log(1L, otherChild, game(2L));
        when(sessionLogRepository.findById(1L)).thenReturn(Optional.of(existing));
        SessionLogRequest request = new SessionLogRequest(2L, LocalDate.now(), Outcome.EASY, 1, null);

        assertThatThrownBy(() -> sessionLogService.update(PARENT_ID, 1L, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("does not belong to you");

        verify(gameRepository, never()).findById(any());
    }

    @Test
    void update_throwsNotFound_whenLogMissing() {
        when(sessionLogRepository.findById(1L)).thenReturn(Optional.empty());
        SessionLogRequest request = new SessionLogRequest(2L, LocalDate.now(), Outcome.EASY, 1, null);

        assertThatThrownBy(() -> sessionLogService.update(PARENT_ID, 1L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Session log not found: 1");
    }

    @Test
    void delete_removesOwnedLog() {
        Child child = childOwnedBy(PARENT_ID, 10L);
        SessionLog existing = log(1L, child, game(2L));
        when(sessionLogRepository.findById(1L)).thenReturn(Optional.of(existing));

        sessionLogService.delete(PARENT_ID, 1L);

        verify(sessionLogRepository).delete(existing);
    }

    @Test
    void delete_throwsAccessDenied_whenNotOwner() {
        Child otherChild = childOwnedBy(999L, 77L);
        SessionLog existing = log(1L, otherChild, game(2L));
        when(sessionLogRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> sessionLogService.delete(PARENT_ID, 1L))
                .isInstanceOf(AccessDeniedException.class);

        verify(sessionLogRepository, never()).delete(any());
    }
}
