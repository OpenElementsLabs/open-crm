package com.openelements.crm.tag;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tags")
@Tag(name = "Tags", description = "Tag management operations")
@SecurityRequirement(name = "oidc")
public class TagController {

    private final TagService tagService;

    public TagController(final TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    @Operation(summary = "List all tags", description = "Returns a paginated list of tags sorted by name")
    @ApiResponse(responseCode = "200", description = "Tags retrieved successfully")
    public Page<TagDto> list(
            @Parameter(description = "Whether to include company/contact counts per tag")
            @RequestParam(defaultValue = "false") final boolean includeCounts,
            @PageableDefault(size = 20, sort = "name") final Pageable pageable) {
        return tagService.findAll(pageable, includeCounts);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get tag by ID")
    @ApiResponse(responseCode = "200", description = "Tag found")
    @ApiResponse(responseCode = "404", description = "Tag not found")
    public TagDto getById(
            @Parameter(description = "Tag ID") @PathVariable final UUID id) {
        return tagService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new tag")
    @ApiResponse(responseCode = "201", description = "Tag created")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "409", description = "Tag with this name already exists")
    public TagDto create(@Valid @RequestBody final TagCreateDto request) {
        return tagService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing tag")
    @ApiResponse(responseCode = "200", description = "Tag updated")
    @ApiResponse(responseCode = "404", description = "Tag not found")
    @ApiResponse(responseCode = "409", description = "Tag with this name already exists")
    public TagDto update(
            @Parameter(description = "Tag ID") @PathVariable final UUID id,
            @Valid @RequestBody final TagCreateDto request) {
        return tagService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a tag")
    @ApiResponse(responseCode = "204", description = "Tag deleted")
    @ApiResponse(responseCode = "404", description = "Tag not found")
    public void delete(
            @Parameter(description = "Tag ID") @PathVariable final UUID id) {
        tagService.delete(id);
    }
}
