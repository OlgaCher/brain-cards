package com.braincards.controller;

import com.braincards.dto.ErrorResponse;
import com.braincards.dto.GameDto;
import com.braincards.model.Child;
import com.braincards.service.ChildService;
import com.braincards.service.DailySuggestionService;
import com.braincards.service.GameService;
import com.braincards.service.ParentUserDetails;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/suggestion")
public class SuggestionRestController {

    private final ChildService childService;
    private final DailySuggestionService dailySuggestionService;
    private final GameService gameService;

    public SuggestionRestController(ChildService childService, DailySuggestionService dailySuggestionService, GameService gameService) {
        this.childService = childService;
        this.dailySuggestionService = dailySuggestionService;
        this.gameService = gameService;
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Today's candidate games for my child", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = GameDto.class)))}),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Basic Auth credentials", content = @Content),
            @ApiResponse(responseCode = "404", description = "This parent has no child yet", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))})
    })
    @GetMapping("/today")
    public List<GameDto> today(@AuthenticationPrincipal ParentUserDetails principal) {
        Child child = childService.findChildEntity(principal.getParentId());
        return dailySuggestionService.suggestGamesForToday(child).stream()
                .map(gameService::toDto)
                .toList();
    }
}
