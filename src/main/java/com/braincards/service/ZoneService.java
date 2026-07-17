package com.braincards.service;

import com.braincards.dto.ZoneDto;
import com.braincards.dto.ZoneRequest;
import com.braincards.dto.ZoneSummaryDto;
import com.braincards.dto.ZoneTranslationRequest;
import com.braincards.model.Zone;
import com.braincards.model.ZoneTranslation;
import com.braincards.repository.GameRepository;
import com.braincards.repository.ZoneRepository;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class ZoneService {

    private static final String FALLBACK_LOCALE = "en";

    private final ZoneRepository zoneRepository;
    private final GameRepository gameRepository;

    public ZoneService(ZoneRepository zoneRepository, GameRepository gameRepository) {
        this.zoneRepository = zoneRepository;
        this.gameRepository = gameRepository;
    }

    public List<ZoneDto> listZones() {
        return zoneRepository.findAll().stream().map(this::toDto).toList();
    }

    public List<ZoneSummaryDto> listZonesWithGameCounts() {
        Locale locale = LocaleContextHolder.getLocale();
        return zoneRepository.findAll().stream()
                .map(zone -> new ZoneSummaryDto(zone.getId(), zone.getCode(),
                        resolveName(zone.getTranslations(), locale).orElse(zone.getCode()),
                        gameRepository.countByZoneIdAndActiveTrue(zone.getId())))
                .toList();
    }

    public ZoneDto getZone(Long id) {
        return toDto(findZoneEntity(id));
    }

    @Transactional
    public ZoneDto createZone(ZoneRequest request) {
        if (zoneRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Zone code already exists: " + request.code());
        }
        Zone zone = new Zone();
        zone.setCode(request.code());
        for (ZoneTranslationRequest t : request.translations()) {
            ZoneTranslation translation = new ZoneTranslation();
            translation.setZone(zone);
            translation.setLocale(t.locale());
            translation.setName(t.name());
            zone.getTranslations().add(translation);
        }
        return toDto(zoneRepository.save(zone));
    }

    private Zone findZoneEntity(Long id) {
        return zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found: " + id));
    }

    public ZoneDto toDto(Zone zone) {
        Locale locale = LocaleContextHolder.getLocale();
        String name = resolveName(zone.getTranslations(), locale).orElse(zone.getCode());
        return new ZoneDto(zone.getId(), zone.getCode(), name);
    }

    private Optional<String> resolveName(List<ZoneTranslation> translations, Locale locale) {
        String language = locale.getLanguage();
        return translations.stream()
                .filter(t -> t.getLocale().equalsIgnoreCase(language))
                .findFirst()
                .map(ZoneTranslation::getName)
                .or(() -> translations.stream()
                        .filter(t -> t.getLocale().equalsIgnoreCase(FALLBACK_LOCALE))
                        .findFirst()
                        .map(ZoneTranslation::getName));
    }
}
