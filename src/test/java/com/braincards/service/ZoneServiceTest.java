package com.braincards.service;

import com.braincards.dto.ZoneDto;
import com.braincards.dto.ZoneRequest;
import com.braincards.dto.ZoneSummaryDto;
import com.braincards.dto.ZoneTranslationRequest;
import com.braincards.model.Zone;
import com.braincards.model.ZoneTranslation;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZoneServiceTest {

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private GameRepository gameRepository;

    @InjectMocks
    private ZoneService zoneService;

    @BeforeEach
    void setLocale() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
    }

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    private ZoneTranslation translation(Zone zone, String locale, String name) {
        ZoneTranslation t = new ZoneTranslation();
        t.setZone(zone);
        t.setLocale(locale);
        t.setName(name);
        return t;
    }

    private Zone zoneWith(Long id, String code, ZoneTranslation... translations) {
        Zone zone = new Zone();
        zone.setId(id);
        zone.setCode(code);
        for (ZoneTranslation t : translations) {
            zone.getTranslations().add(t);
        }
        return zone;
    }

    @Test
    void toDto_usesTranslationMatchingCurrentLocale() {
        LocaleContextHolder.setLocale(new Locale("uk"));
        Zone zone = new Zone();
        zone.setId(1L);
        zone.setCode("MOTOR");
        zone.getTranslations().add(translation(zone, "en", "Motor skills"));
        zone.getTranslations().add(translation(zone, "uk", "Моторика"));

        ZoneDto dto = zoneService.toDto(zone);

        assertThat(dto.name()).isEqualTo("Моторика");
    }

    @Test
    void toDto_fallsBackToEnglish_whenLocaleMissing() {
        LocaleContextHolder.setLocale(new Locale("fr"));
        Zone zone = new Zone();
        zone.setId(1L);
        zone.setCode("MOTOR");
        zone.getTranslations().add(translation(zone, "en", "Motor skills"));

        ZoneDto dto = zoneService.toDto(zone);

        assertThat(dto.name()).isEqualTo("Motor skills");
    }

    @Test
    void toDto_fallsBackToCode_whenNoTranslationsAtAll_edgeCase() {
        Zone zone = new Zone();
        zone.setId(1L);
        zone.setCode("MOTOR");

        ZoneDto dto = zoneService.toDto(zone);

        assertThat(dto.name()).isEqualTo("MOTOR");
    }

    @Test
    void listZones_mapsEveryZone() {
        Zone z1 = zoneWith(1L, "A", translation(null, "en", "Alpha"));
        Zone z2 = zoneWith(2L, "B", translation(null, "en", "Beta"));
        when(zoneRepository.findAll()).thenReturn(List.of(z1, z2));

        List<ZoneDto> result = zoneService.listZones();

        assertThat(result).extracting(ZoneDto::name).containsExactly("Alpha", "Beta");
    }

    @Test
    void listZones_returnsEmpty_whenNoZones_edgeCase() {
        when(zoneRepository.findAll()).thenReturn(List.of());

        assertThat(zoneService.listZones()).isEmpty();
    }

    @Test
    void listZonesWithGameCounts_includesActiveGameCountPerZone() {
        Zone z1 = zoneWith(1L, "A", translation(null, "en", "Alpha"));
        when(zoneRepository.findAll()).thenReturn(List.of(z1));
        when(gameRepository.countByZoneIdAndActiveTrue(1L)).thenReturn(3L);

        List<ZoneSummaryDto> result = zoneService.listZonesWithGameCounts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).gameCount()).isEqualTo(3L);
        assertThat(result.get(0).name()).isEqualTo("Alpha");
    }

    @Test
    void getZone_returnsDto_whenFound() {
        Zone zone = zoneWith(7L, "CODE", translation(null, "en", "Name"));
        when(zoneRepository.findById(7L)).thenReturn(Optional.of(zone));

        assertThat(zoneService.getZone(7L).id()).isEqualTo(7L);
    }

    @Test
    void getZone_throwsNotFound_whenMissing() {
        when(zoneRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> zoneService.getZone(7L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Zone not found: 7");
    }

    @Test
    void createZone_persistsZoneWithTranslations() {
        ZoneRequest request = new ZoneRequest("NEW",
                List.of(new ZoneTranslationRequest("en", "Fresh")));
        when(zoneRepository.existsByCode("NEW")).thenReturn(false);
        when(zoneRepository.save(any(Zone.class))).thenAnswer(inv -> {
            Zone z = inv.getArgument(0);
            z.setId(50L);
            return z;
        });

        ZoneDto dto = zoneService.createZone(request);

        assertThat(dto.id()).isEqualTo(50L);
        assertThat(dto.code()).isEqualTo("NEW");
        assertThat(dto.name()).isEqualTo("Fresh");
    }

    @Test
    void createZone_throwsIllegalArgument_whenCodeExists() {
        ZoneRequest request = new ZoneRequest("DUP",
                List.of(new ZoneTranslationRequest("en", "Dup")));
        when(zoneRepository.existsByCode("DUP")).thenReturn(true);

        assertThatThrownBy(() -> zoneService.createZone(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        verify(zoneRepository, never()).save(any());
    }
}
