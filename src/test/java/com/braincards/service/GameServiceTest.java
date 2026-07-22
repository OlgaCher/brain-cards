package com.braincards.service;

import com.braincards.dto.GameDto;
import com.braincards.dto.GameRequest;
import com.braincards.dto.GameTranslationRequest;
import com.braincards.dto.ZoneDto;
import com.braincards.model.Game;
import com.braincards.model.GameTranslation;
import com.braincards.model.Zone;
import com.braincards.repository.GameRepository;
import com.braincards.repository.ZoneRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private ZoneService zoneService;

    @InjectMocks
    private GameService gameService;

    @BeforeEach
    void setup() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        lenient().when(zoneService.toDto(any(Zone.class)))
                .thenReturn(new ZoneDto(1L, "ZONE", "Zone Name"));
    }

    @AfterEach
    void reset() {
        LocaleContextHolder.resetLocaleContext();
    }

    private Zone zone(Long id) {
        Zone zone = new Zone();
        zone.setId(id);
        zone.setCode("ZONE");
        return zone;
    }

    private GameTranslation translation(Game game, String locale, String title, String instructions) {
        GameTranslation t = new GameTranslation();
        t.setGame(game);
        t.setLocale(locale);
        t.setTitle(title);
        t.setInstructions(instructions);
        return t;
    }

    private Game game(Long id, Zone zone) {
        Game game = new Game();
        game.setId(id);
        game.setZone(zone);
        game.setMinAgeMonths(12);
        game.setMaxAgeMonths(36);
        game.setActive(true);
        game.setCooldownDays(7);
        return game;
    }

    @Test
    void listGames_filtersByZone_whenZoneIdProvided() {
        Game g = game(1L, zone(1L));
        when(gameRepository.findByZoneId(1L)).thenReturn(List.of(g));

        List<GameDto> result = gameService.listGames(1L);

        assertThat(result).hasSize(1);
        verify(gameRepository).findByZoneId(1L);
        verify(gameRepository, never()).findAll();
    }

    @Test
    void listGames_returnsAll_whenZoneIdNull() {
        when(gameRepository.findAll()).thenReturn(List.of(game(1L, zone(1L)), game(2L, zone(1L))));

        List<GameDto> result = gameService.listGames(null);

        assertThat(result).hasSize(2);
        verify(gameRepository).findAll();
    }

    @Test
    void toDto_resolvesLocalizedTitle() {
        LocaleContextHolder.setLocale(new Locale("uk"));
        Game g = game(1L, zone(1L));
        g.getTranslations().add(translation(g, "en", "Peekaboo", "Hide"));
        g.getTranslations().add(translation(g, "uk", "Ку-ку", "Ховатися"));

        GameDto dto = gameService.toDto(g);

        assertThat(dto.title()).isEqualTo("Ку-ку");
        assertThat(dto.instructions()).isEqualTo("Ховатися");
        assertThat(dto.zoneName()).isEqualTo("Zone Name");
    }

    @Test
    void toDto_fallsBackToEnglish_whenLocaleMissing() {
        LocaleContextHolder.setLocale(new Locale("fr"));
        Game g = game(1L, zone(1L));
        g.getTranslations().add(translation(g, "en", "Peekaboo", "Hide"));

        GameDto dto = gameService.toDto(g);

        assertThat(dto.title()).isEqualTo("Peekaboo");
    }

    @Test
    void toDto_nullTitle_whenNoTranslations_edgeCase() {
        Game g = game(1L, zone(1L));

        GameDto dto = gameService.toDto(g);

        assertThat(dto.title()).isNull();
        assertThat(dto.instructions()).isNull();
    }

    @Test
    void getGame_throwsNotFound_whenMissing() {
        when(gameRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.getGame(9L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Game not found: 9");
    }

    @Test
    void createGame_persistsWithTranslationsAndZone() {
        GameRequest request = new GameRequest(1L, 6, 24, true, 5,
                List.of(new GameTranslationRequest("en", "Stack", "Stack blocks")));
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone(1L)));
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> {
            Game g = inv.getArgument(0);
            g.setId(100L);
            return g;
        });

        GameDto dto = gameService.createGame(request);

        assertThat(dto.id()).isEqualTo(100L);
        assertThat(dto.minAgeMonths()).isEqualTo(6);
        assertThat(dto.maxAgeMonths()).isEqualTo(24);
        assertThat(dto.active()).isTrue();
        assertThat(dto.cooldownDays()).isEqualTo(5);
        assertThat(dto.title()).isEqualTo("Stack");
    }

    @Test
    void createGame_defaultsActiveToTrue_whenNull_edgeCase() {
        GameRequest request = new GameRequest(1L, 6, 24, null, null,
                List.of(new GameTranslationRequest("en", "Stack", null)));
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone(1L)));
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

        GameDto dto = gameService.createGame(request);

        assertThat(dto.active()).isTrue();
        assertThat(dto.cooldownDays()).isNull();
    }

    @Test
    void createGame_throwsNotFound_whenZoneMissing() {
        GameRequest request = new GameRequest(99L, 6, 24, true, 5,
                List.of(new GameTranslationRequest("en", "Stack", "x")));
        when(zoneRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.createGame(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Zone not found: 99");

        verify(gameRepository, never()).save(any());
    }

    @Test
    void updateGame_replacesTranslationsAndReassignsZone() {
        Game existing = game(1L, zone(1L));
        existing.getTranslations().add(translation(existing, "en", "Old", "old"));
        Zone newZone = zone(2L);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(zoneRepository.findById(2L)).thenReturn(Optional.of(newZone));
        GameRequest request = new GameRequest(2L, 10, 30, false, 3,
                List.of(new GameTranslationRequest("en", "New", "new")));

        GameDto dto = gameService.updateGame(1L, request);

        assertThat(existing.getZone()).isSameAs(newZone);
        assertThat(existing.getTranslations()).hasSize(1);
        assertThat(existing.getTranslations().get(0).getTitle()).isEqualTo("New");
        assertThat(dto.active()).isFalse();
        assertThat(dto.minAgeMonths()).isEqualTo(10);
        verify(gameRepository, never()).save(any());
    }

    @Test
    void updateGame_throwsNotFound_whenGameMissing() {
        when(gameRepository.findById(1L)).thenReturn(Optional.empty());
        GameRequest request = new GameRequest(1L, 10, 30, false, 3,
                List.of(new GameTranslationRequest("en", "New", "new")));

        assertThatThrownBy(() -> gameService.updateGame(1L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Game not found");
    }

    @Test
    void deleteGame_deletesResolvedEntity() {
        Game existing = game(1L, zone(1L));
        when(gameRepository.findById(1L)).thenReturn(Optional.of(existing));

        gameService.deleteGame(1L);

        verify(gameRepository).delete(existing);
    }

    @Test
    void deleteGame_throwsNotFound_whenMissing() {
        when(gameRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.deleteGame(1L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(gameRepository, never()).delete(any());
    }
}
