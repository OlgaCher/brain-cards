package com.braincards.controller;

import com.braincards.dto.ErrorResponse;
import com.braincards.dto.ZoneDto;
import com.braincards.dto.ZoneRequest;
import com.braincards.service.ZoneService;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/zone")
public class ZoneRestController {

    private final ZoneService zoneService;

    public ZoneRestController(ZoneService zoneService) {
        this.zoneService = zoneService;
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Zones, localized name for the current locale", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = ZoneDto.class)))}),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Basic Auth credentials", content = @Content)
    })
    @GetMapping
    public List<ZoneDto> list() {
        return zoneService.listZones();
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The zone", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ZoneDto.class))}),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Basic Auth credentials", content = @Content),
            @ApiResponse(responseCode = "404", description = "No zone with this id", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))})
    })
    @GetMapping("/{id}")
    public ZoneDto get(@PathVariable Long id) {
        return zoneService.getZone(id);
    }

    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Zone created", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ZoneDto.class))}),
            @ApiResponse(responseCode = "400", description = "Validation failed, or this code is already in use", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))}),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Basic Auth credentials", content = @Content)
    })
    @PostMapping
    public ResponseEntity<ZoneDto> create(@Valid @RequestBody ZoneRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(zoneService.createZone(request));
    }
}
