package com.braincards.controller;

import com.braincards.dto.ParentDto;
import com.braincards.service.ParentService;
import com.braincards.service.ParentUserDetails;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/parent")
public class ParentRestController {

    private final ParentService parentService;

    public ParentRestController(ParentService parentService) {
        this.parentService = parentService;
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current parent's profile", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ParentDto.class))}),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Basic Auth credentials", content = @Content)
    })
    @GetMapping("/me")
    public ParentDto me(@AuthenticationPrincipal ParentUserDetails principal) {
        return parentService.getProfile(principal.getParentId());
    }
}
