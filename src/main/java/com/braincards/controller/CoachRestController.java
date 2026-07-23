package com.braincards.controller;

import com.braincards.ai.AiCoachService;
import com.braincards.dto.CoachRequest;
import com.braincards.dto.CoachResponse;
import com.braincards.dto.ErrorResponse;
import com.braincards.model.Child;
import com.braincards.service.ChildService;
import com.braincards.service.ParentUserDetails;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/coach")
public class CoachRestController {

    private final AiCoachService aiCoachService;
    private final ChildService childService;

    public CoachRestController(AiCoachService aiCoachService, ChildService childService) {
        this.aiCoachService = aiCoachService;
        this.childService = childService;
    }

    // The child is resolved from the authenticated parent (ownership boundary) - the request only
    // supplies which game the question is about, never whose child.
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Coaching answer for my child and the given game", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CoachResponse.class))}),
            @ApiResponse(responseCode = "400", description = "Validation failed (missing gameId or blank question)", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))}),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Basic Auth credentials", content = @Content),
            @ApiResponse(responseCode = "404", description = "This parent has no child yet, or no game with the given gameId", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))}),
            @ApiResponse(responseCode = "503", description = "AI service unavailable or not configured", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))})
    })
    @PostMapping
    public CoachResponse explain(@AuthenticationPrincipal ParentUserDetails principal,
                                 @Valid @RequestBody CoachRequest request) {
        Child child = childService.findChildEntity(principal.getParentId());
        String answer = aiCoachService.explainOrCoach(child.getId(), request.gameId(), request.question());
        return new CoachResponse(answer);
    }
}
