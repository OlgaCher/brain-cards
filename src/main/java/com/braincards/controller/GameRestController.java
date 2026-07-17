package com.braincards.controller;

import com.braincards.dto.ErrorResponse;
import com.braincards.dto.GameDto;
import com.braincards.dto.GameRequest;
import com.braincards.service.GameService;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/game")
public class GameRestController {

    private final GameService gameService;

    public GameRestController(GameService gameService) {
        this.gameService = gameService;
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Games, optionally filtered by zone", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = GameDto.class)))}),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Basic Auth credentials", content = @Content)
    })
    @GetMapping
    public List<GameDto> list(@RequestParam(required = false) Long zoneId) {
        return gameService.listGames(zoneId);
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The game", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GameDto.class))}),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Basic Auth credentials", content = @Content),
            @ApiResponse(responseCode = "404", description = "No game with this id", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))})
    })
    @GetMapping("/{id}")
    public GameDto get(@PathVariable Long id) {
        return gameService.getGame(id);
    }

    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Game created", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GameDto.class))}),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))}),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Basic Auth credentials", content = @Content),
            @ApiResponse(responseCode = "404", description = "No zone with the given zoneId", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))})
    })
    @PostMapping
    public ResponseEntity<GameDto> create(@Valid @RequestBody GameRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gameService.createGame(request));
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Game updated", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GameDto.class))}),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))}),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Basic Auth credentials", content = @Content),
            @ApiResponse(responseCode = "404", description = "No game with this id, or no zone with the given zoneId", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))})
    })
    @PutMapping("/{id}")
    public GameDto update(@PathVariable Long id, @Valid @RequestBody GameRequest request) {
        return gameService.updateGame(id, request);
    }

    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Game deleted", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Basic Auth credentials", content = @Content),
            @ApiResponse(responseCode = "404", description = "No game with this id", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))})
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        gameService.deleteGame(id);
        return ResponseEntity.noContent().build();
    }
}
