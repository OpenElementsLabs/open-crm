package com.openelements.crm.comment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for comment update and delete operations.
 * Comment creation is handled by {@link com.openelements.crm.company.CompanyController}
 * and {@link com.openelements.crm.contact.ContactController}.
 */
@RestController
@RequestMapping("/api/comments")
@Tag(name = "Comments", description = "Comment management operations")
public class CommentController {

    private final CommentService commentService;

    /**
     * Creates a new CommentController.
     *
     * @param commentService the comment service
     */
    public CommentController(final CommentService commentService) {
        this.commentService = Objects.requireNonNull(commentService, "commentService must not be null");
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
    public CommentResponse update(@PathVariable final UUID id,
                                  @Valid @RequestBody final CommentUpdateRequest request) {
        return commentService.update(id, request);
    }

    /**
     * Deletes a comment.
     *
     * @param id the comment ID
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a comment")
    @ApiResponse(responseCode = "204", description = "Comment deleted")
    @ApiResponse(responseCode = "404", description = "Comment not found")
    public void delete(@PathVariable final UUID id) {
        commentService.delete(id);
    }
}
