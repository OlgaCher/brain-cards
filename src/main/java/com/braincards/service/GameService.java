package com.braincards.service;

import com.braincards.dto.GameDto;
import com.braincards.dto.GameRequest;
import com.braincards.dto.GameTranslationRequest;
import com.braincards.model.Game;
import com.braincards.model.GameTranslation;
import com.braincards.model.Zone;
import com.braincards.repository.GameRepository;
import com.braincards.repository.ZoneRepository;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class GameService {

    private static final String FALLBACK_LOCALE = "en";

    private final GameRepository gameRepository;
    private final ZoneRepository zoneRepository;
    private final ZoneService zoneService;

    public GameService(GameRepository gameRepository, ZoneRepository zoneRepository, ZoneService zoneService) {
        this.gameRepository = gameRepository;
        this.zoneRepository = zoneRepository;
        this.zoneService = zoneService;
    }

    public List<GameDto> listGames(Long zoneId) {
        List<Game> games = zoneId != null ? gameRepository.findByZoneId(zoneId) : gameRepository.findAll();
        return games.stream().map(this::toDto).toList();
    }

    public GameDto getGame(Long id) {
        return toDto(findGameEntity(id));
    }

    @Transactional
    public GameDto createGame(GameRequest request) {
        Zone zone = zoneRepository.findById(request.zoneId())
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found: " + request.zoneId()));

        Game game = new Game();
        game.setZone(zone);
        applyRequest(game, request);
        return toDto(gameRepository.save(game));
    }

    @Transactional
    public GameDto updateGame(Long id, GameRequest request) {
        Game game = findGameEntity(id);
        Zone zone = zoneRepository.findById(request.zoneId())
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found: " + request.zoneId()));
        game.setZone(zone);
        applyRequest(game, request);
        return toDto(game);
    }

    @Transactional
    public void deleteGame(Long id) {
        gameRepository.delete(findGameEntity(id));
    }

    // Reused by DailySuggestionService's caller to map candidate games to the same localized shape.
    public GameDto toDto(Game game) {
        Locale locale = LocaleContextHolder.getLocale();
        GameTranslation translation = resolveTranslation(game.getTranslations(), locale).orElse(null);
        String title = translation != null ? translation.getTitle() : null;
        String instructions = translation != null ? translation.getInstructions() : null;
        String zoneName = zoneService.toDto(game.getZone()).name();
        return new GameDto(game.getId(), game.getZone().getId(), zoneName, game.getMinAgeMonths(), game.getMaxAgeMonths(),
                game.isActive(), game.getCooldownDays(), title, instructions);
    }

    private void applyRequest(Game game, GameRequest request) {
        game.setMinAgeMonths(request.minAgeMonths());
        game.setMaxAgeMonths(request.maxAgeMonths());
        game.setActive(request.active() == null || request.active());
        game.setCooldownDays(request.cooldownDays());

        game.getTranslations().clear();
        for (GameTranslationRequest t : request.translations()) {
            GameTranslation translation = new GameTranslation();
            translation.setGame(game);
            translation.setLocale(t.locale());
            translation.setTitle(t.title());
            translation.setInstructions(t.instructions());
            game.getTranslations().add(translation);
        }
    }

    private Game findGameEntity(Long id) {
        return gameRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Game not found: " + id));
    }

    private Optional<GameTranslation> resolveTranslation(List<GameTranslation> translations, Locale locale) {
        String language = locale.getLanguage();
        return translations.stream()
                .filter(t -> t.getLocale().equalsIgnoreCase(language))
                .findFirst()
                .or(() -> translations.stream()
                        .filter(t -> t.getLocale().equalsIgnoreCase(FALLBACK_LOCALE))
                        .findFirst());
    }
}
