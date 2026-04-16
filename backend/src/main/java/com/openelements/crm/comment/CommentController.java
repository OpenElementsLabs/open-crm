package com.openelements.crm.comment;

import com.openelements.crm.user.UserController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.UUID;

/**
 * REST controller for comment update and delete operations.
 * Comment creation is handled by {@link com.openelements.crm.company.CompanyController}
 * and {@link com.openelements.crm.contact.ContactController}.
 */
@RestController
@RequestMapping("/api/comments")
@Tag(name = "Comments", description = "Comment management operations")
@SecurityRequirement(name = "oidc")
public class CommentController {

    private final CommentService commentService;

    private final UserController userController;

    /**
     * Creates a new CommentController.
     *
     * @param commentService the comment service
     */
    public CommentController(final CommentService commentService, final UserController userController) {
        this.commentService = Objects.requireNonNull(commentService, "commentService must not be null");
        this.userController = Objects.requireNonNull(userController, "userController must not be null");
    }

    /**
     * Updates an existing comment.
     *
     * @param id      the comment ID
     * @param request the update request
     * @return the updated comment response
     */
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a comment")
    @ApiResponse(responseCode = "200", description = "Comment updated")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "404", description = "Comment not found")
    public CommentDto update(@Parameter(description = "The comment ID") @PathVariable final UUID id,
                             @Valid @RequestBody final CommentUpdateDto request) {
        CommentDto base = commentService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
        CommentDto updated = new CommentDto(base.id(),
            request.text(),
            base.author(),
            base.companyId(),
            base.contactId(),
            base.taskId(),
            base.createdAt(),
            base.updatedAt());
        return commentService.save(updated);
    }

    /**
     * Deletes a comment.
     *
     * @param id the comment ID
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a comment")
    @ApiResponse(responseCode = "204", description = "Comment deleted")
    @ApiResponse(responseCode = "403", description = "Missing ADMIN role")
    @ApiResponse(responseCode = "404", description = "Comment not found")
    public void delete(@Parameter(description = "The comment ID") @PathVariable final UUID id) {
        commentService.delete(id);
    }
}
