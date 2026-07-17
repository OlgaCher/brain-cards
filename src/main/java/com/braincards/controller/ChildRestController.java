package com.braincards.controller;

import com.braincards.dto.ChildDto;
import com.braincards.dto.ChildRequest;
import com.braincards.dto.ErrorResponse;
import com.braincards.service.ChildService;
import com.braincards.service.ParentUserDetails;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/child")
public class ChildRestController {

    private final ChildService childService;

    public ChildRestController(ChildService childService) {
        this.childService = childService;
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "My child", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ChildDto.class))}),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Basic Auth credentials", content = @Content),
            @ApiResponse(responseCode = "404", description = "This parent has no child yet", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))})
    })
    @GetMapping
    public ChildDto getMine(@AuthenticationPrincipal ParentUserDetails principal) {
        return childService.getMyChild(principal.getParentId());
    }

    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Child created", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ChildDto.class))}),
            @ApiResponse(responseCode = "400", description = "Validation failed, or this parent already has a child", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))}),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Basic Auth credentials", content = @Content)
    })
    @PostMapping
    public ResponseEntity<ChildDto> create(@AuthenticationPrincipal ParentUserDetails principal,
                                            @Valid @RequestBody ChildRequest request) {
        ChildDto created = childService.createChild(principal.getParentId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Child updated", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ChildDto.class))}),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))}),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Basic Auth credentials", content = @Content),
            @ApiResponse(responseCode = "404", description = "This parent has no child yet", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))})
    })
    @PutMapping
    public ChildDto update(@AuthenticationPrincipal ParentUserDetails principal,
                            @Valid @RequestBody ChildRequest request) {
        return childService.updateChild(principal.getParentId(), request);
    }

    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Child deleted", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Basic Auth credentials", content = @Content),
            @ApiResponse(responseCode = "404", description = "This parent has no child yet", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))})
    })
    @DeleteMapping
    public ResponseEntity<Void> delete(@AuthenticationPrincipal ParentUserDetails principal) {
        childService.deleteChild(principal.getParentId());
        return ResponseEntity.noContent().build();
    }
}
