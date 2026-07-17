package com.braincards.service;

import com.braincards.dto.SessionLogDto;
import com.braincards.dto.SessionLogRequest;
import com.braincards.model.Child;
import com.braincards.model.Game;
import com.braincards.model.SessionLog;
import com.braincards.repository.GameRepository;
import com.braincards.repository.SessionLogRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class SessionLogService {

    private final SessionLogRepository sessionLogRepository;
    private final GameRepository gameRepository;
    private final ChildService childService;

    public SessionLogService(SessionLogRepository sessionLogRepository, GameRepository gameRepository, ChildService childService) {
        this.sessionLogRepository = sessionLogRepository;
        this.gameRepository = gameRepository;
        this.childService = childService;
    }

    public List<SessionLogDto> listMine(Long parentId, LocalDate date) {
        Child child = childService.findChildEntity(parentId);
        List<SessionLog> logs = date != null
                ? sessionLogRepository.findByChildIdAndPlayedOn(child.getId(), date)
                : sessionLogRepository.findByChildId(child.getId());
        return logs.stream().map(this::toDto).toList();
    }

    @Transactional
    public SessionLogDto create(Long parentId, SessionLogRequest request) {
        Child child = childService.findChildEntity(parentId);
        Game game = gameRepository.findById(request.gameId())
                .orElseThrow(() -> new ResourceNotFoundException("Game not found: " + request.gameId()));

        SessionLog sessionLog = new SessionLog();
        sessionLog.setChild(child);
        sessionLog.setGame(game);
        applyRequest(sessionLog, request);
        return toDto(sessionLogRepository.save(sessionLog));
    }

    @Transactional
    public SessionLogDto update(Long parentId, Long sessionLogId, SessionLogRequest request) {
        SessionLog sessionLog = findOwned(parentId, sessionLogId);
        Game game = gameRepository.findById(request.gameId())
                .orElseThrow(() -> new ResourceNotFoundException("Game not found: " + request.gameId()));
        sessionLog.setGame(game);
        applyRequest(sessionLog, request);
        return toDto(sessionLog);
    }

    @Transactional
    public void delete(Long parentId, Long sessionLogId) {
        sessionLogRepository.delete(findOwned(parentId, sessionLogId));
    }

    // A session log id is client-supplied (unlike "my child"), so ownership must be checked
    // explicitly here rather than assumed from how the record was looked up.
    private SessionLog findOwned(Long parentId, Long sessionLogId) {
        SessionLog sessionLog = sessionLogRepository.findById(sessionLogId)
                .orElseThrow(() -> new ResourceNotFoundException("Session log not found: " + sessionLogId));
        if (!sessionLog.getChild().getParent().getId().equals(parentId)) {
            throw new AccessDeniedException("This session log does not belong to you");
        }
        return sessionLog;
    }

    private void applyRequest(SessionLog sessionLog, SessionLogRequest request) {
        sessionLog.setPlayedOn(request.playedOn());
        sessionLog.setOutcome(request.outcome());
        sessionLog.setDurationMin(request.durationMin());
        sessionLog.setParentNote(request.parentNote());
    }

    private SessionLogDto toDto(SessionLog sessionLog) {
        return new SessionLogDto(sessionLog.getId(), sessionLog.getChild().getId(), sessionLog.getGame().getId(),
                sessionLog.getPlayedOn(), sessionLog.getOutcome(), sessionLog.getDurationMin(), sessionLog.getParentNote());
    }
}
