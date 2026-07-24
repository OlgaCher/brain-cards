package com.braincards.controller;

import com.braincards.ai.AiCoachException;
import com.braincards.ai.AiCoachService;
import com.braincards.dto.GameDto;
import com.braincards.dto.SessionLogDto;
import com.braincards.dto.SessionLogRequest;
import com.braincards.dto.ZoneDto;
import com.braincards.dto.ZoneSummaryDto;
import com.braincards.model.Child;
import com.braincards.model.Game;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private final ParentRepository parentRepository;
    private final DailySuggestionService dailySuggestionService;
    private final GameService gameService;
    private final ZoneService zoneService;
    private final SessionLogService sessionLogService;
    private final AiCoachService aiCoachService;

    public HomeController(ParentRepository parentRepository, DailySuggestionService dailySuggestionService,
                           GameService gameService, ZoneService zoneService, SessionLogService sessionLogService,
                           AiCoachService aiCoachService) {
        this.parentRepository = parentRepository;
        this.dailySuggestionService = dailySuggestionService;
        this.gameService = gameService;
        this.zoneService = zoneService;
        this.sessionLogService = sessionLogService;
        this.aiCoachService = aiCoachService;
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

        model.addAttribute("noGamesForAge",
                child != null && todaysGames.isEmpty() && !dailySuggestionService.hasGamesForAge(child));

        List<ZoneSummaryDto> zones = zoneService.listZonesWithGameCounts();
        model.addAttribute("zones", zones);

        return "home";
    }

    @GetMapping("/home/zones/{zoneId}")
    public String zoneDetail(@AuthenticationPrincipal ParentUserDetails principal, @PathVariable Long zoneId, Model model) {
        ZoneDto zone = zoneService.getZone(zoneId); // 404 if the zone doesn't exist
        List<GameDto> games = gameService.listGames(zoneId).stream().filter(GameDto::active).toList();

        Child child = parentRepository.findById(principal.getParentId()).orElseThrow().getChild();
        Long currentPickId = child == null ? null
                : dailySuggestionService.suggestGamesForToday(child).stream()
                        .filter(game -> game.getZone().getId().equals(zoneId))
                        .map(Game::getId)
                        .findFirst()
                        .orElse(null);

        Set<Long> playedTodayIds = child == null ? Set.of()
                : sessionLogService.listMine(principal.getParentId(), LocalDate.now()).stream()
                        .map(SessionLogDto::gameId)
                        .collect(Collectors.toSet());
        boolean zoneDoneToday = games.stream().anyMatch(game -> playedTodayIds.contains(game.id()));

        model.addAttribute("zoneName", zone.name());
        model.addAttribute("zoneId", zoneId);
        model.addAttribute("games", games);
        model.addAttribute("accentIndex", accentIndexOf(zoneId));
        model.addAttribute("currentPickId", currentPickId);
        model.addAttribute("playedTodayIds", playedTodayIds);
        model.addAttribute("zoneDoneToday", zoneDoneToday);
        model.addAttribute("hasChild", child != null);
        return "zone";
    }

    @PostMapping("/home/games/{gameId}/complete")
    public String markComplete(@AuthenticationPrincipal ParentUserDetails principal, @PathVariable Long gameId) {
        sessionLogService.create(principal.getParentId(),
                new SessionLogRequest(gameId, LocalDate.now(), Outcome.JUST_RIGHT, null, null));
        return "redirect:/home";
    }

    @PostMapping("/home/games/{gameId}/swap")
    public String swap(@AuthenticationPrincipal ParentUserDetails principal, @PathVariable Long gameId) {
        Child child = parentRepository.findById(principal.getParentId()).orElseThrow().getChild();
        if (child != null) {
            dailySuggestionService.recordSwap(child, gameId);
        }
        return "redirect:/home";
    }

    @PostMapping("/home/games/{gameId}/choose")
    public String choose(@AuthenticationPrincipal ParentUserDetails principal, @PathVariable Long gameId) {
        Child child = parentRepository.findById(principal.getParentId()).orElseThrow().getChild();
        if (child != null) {
            dailySuggestionService.choosePickForToday(child, gameId);
        }
        return "redirect:/home";
    }

    @GetMapping("/home/games/{gameId}/coach")
    public String coachPage(@PathVariable Long gameId, Model model) {
        GameDto game = gameService.getGame(gameId); // 404 if the game doesn't exist
        model.addAttribute("game", game);
        model.addAttribute("accentIndex", accentIndexOf(game.zoneId()));
        return "coach";
    }

    @PostMapping("/home/games/{gameId}/coach")
    public String askCoach(@AuthenticationPrincipal ParentUserDetails principal,
                            @PathVariable Long gameId,
                            @RequestParam String question,
                            RedirectAttributes redirectAttributes) {
        Child child = parentRepository.findById(principal.getParentId()).orElseThrow().getChild();
        if (child == null) {
            return "redirect:/home";
        }
        redirectAttributes.addFlashAttribute("question", question);
        try {
            redirectAttributes.addFlashAttribute("answer",
                    aiCoachService.explainOrCoach(child.getId(), gameId, question));
        } catch (AiCoachException e) {
            redirectAttributes.addFlashAttribute("coachError", e.getMessage());
        }
        return "redirect:/home/games/" + gameId + "/coach";
    }

    private int accentIndexOf(Long zoneId) {
        return (int) (zoneId % 5);
    }
}
