package com.braincards.controller;

import com.braincards.dto.ErrorResponse;
import com.braincards.dto.SessionLogDto;
import com.braincards.dto.SessionLogRequest;
import com.braincards.service.ParentUserDetails;
import com.braincards.service.SessionLogService;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/session-log")
public class SessionLogRestController {

    private final SessionLogService sessionLogService;

    public SessionLogRestController(SessionLogService sessionLogService) {
        this.sessionLogService = sessionLogService;
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "My session logs, optionally filtered by date", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = SessionLogDto.class)))}),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Basic Auth credentials", content = @Content)
    })
    @GetMapping
    public List<SessionLogDto> list(@AuthenticationPrincipal ParentUserDetails principal,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return sessionLogService.listMine(principal.getParentId(), date);
    }

    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Session log created", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SessionLogDto.class))}),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))}),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Basic Auth credentials", content = @Content),
            @ApiResponse(responseCode = "404", description = "No child yet, or no game with the given gameId", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))})
    })
    @PostMapping
    public ResponseEntity<SessionLogDto> create(@AuthenticationPrincipal ParentUserDetails principal,
                                                 @Valid @RequestBody SessionLogRequest request) {
        SessionLogDto created = sessionLogService.create(principal.getParentId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session log updated", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SessionLogDto.class))}),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))}),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Basic Auth credentials", content = @Content),
            @ApiResponse(responseCode = "403", description = "This session log belongs to a different parent", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))}),
            @ApiResponse(responseCode = "404", description = "No session log with this id, or no game with the given gameId", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))})
    })
    @PutMapping("/{id}")
    public SessionLogDto update(@AuthenticationPrincipal ParentUserDetails principal,
                                 @PathVariable Long id,
                                 @Valid @RequestBody SessionLogRequest request) {
        return sessionLogService.update(principal.getParentId(), id, request);
    }

    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Session log deleted", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Basic Auth credentials", content = @Content),
            @ApiResponse(responseCode = "403", description = "This session log belongs to a different parent", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))}),
            @ApiResponse(responseCode = "404", description = "No session log with this id", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))})
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal ParentUserDetails principal, @PathVariable Long id) {
        sessionLogService.delete(principal.getParentId(), id);
        return ResponseEntity.noContent().build();
    }
}
