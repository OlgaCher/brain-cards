package com.braincards.controller;

import com.braincards.dto.GameDto;
import com.braincards.dto.SessionLogRequest;
import com.braincards.dto.ZoneSummaryDto;
import com.braincards.model.Child;
import com.braincards.model.Outcome;
import com.braincards.model.Parent;
import com.braincards.repository.ParentRepository;
import com.braincards.service.DailySuggestionService;
import com.braincards.service.GameService;
import com.braincards.service.ParentUserDetails;
import com.braincards.service.SessionLogService;
import com.braincards.service.ZoneService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDate;
import java.util.List;

@Controller
public class HomeController {

    private final ParentRepository parentRepository;
    private final DailySuggestionService dailySuggestionService;
    private final GameService gameService;
    private final ZoneService zoneService;
    private final SessionLogService sessionLogService;

    public HomeController(ParentRepository parentRepository, DailySuggestionService dailySuggestionService,
                           GameService gameService, ZoneService zoneService, SessionLogService sessionLogService) {
        this.parentRepository = parentRepository;
        this.dailySuggestionService = dailySuggestionService;
        this.gameService = gameService;
        this.zoneService = zoneService;
        this.sessionLogService = sessionLogService;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/home";
    }

    @GetMapping("/home")
    public String home(@AuthenticationPrincipal ParentUserDetails principal, Model model) {
        Parent parent = parentRepository.findById(principal.getParentId()).orElseThrow();
        Child child = parent.getChild();
        model.addAttribute("parent", parent);
        model.addAttribute("child", child);

        List<GameDto> todaysGames = child != null
                ? dailySuggestionService.suggestGamesForToday(child).stream().map(gameService::toDto).toList()
                : List.of();
        model.addAttribute("todaysGames", todaysGames);

        List<ZoneSummaryDto> zones = zoneService.listZonesWithGameCounts();
        model.addAttribute("zones", zones);

        return "home";
    }

    // Simple "mark as done" for the happy path - always logs JUST_RIGHT with no duration/note.
    // A fuller outcome-picker (EASY / JUST_RIGHT / TOO_HARD / REFUSED, duration, notes) is a
    // planned follow-up; for now this is enough to remove the game from today's suggestions
    // (the cooldown query excludes anything with a session log dated today).
    @PostMapping("/home/games/{gameId}/complete")
    public String markComplete(@AuthenticationPrincipal ParentUserDetails principal, @PathVariable Long gameId) {
        sessionLogService.create(principal.getParentId(),
                new SessionLogRequest(gameId, LocalDate.now(), Outcome.JUST_RIGHT, null, null));
        return "redirect:/home";
    }
}
