package com.openelements.crm.task;

import com.openelements.crm.comment.CommentCreateDto;
import com.openelements.crm.comment.CommentDto;
import com.openelements.crm.comment.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
@RequestMapping("/api/tasks")
@Tag(name = "Tasks")
public class TaskController {

    private final TaskService taskService;
    private final CommentService commentService;

    public TaskController(final TaskService taskService, final CommentService commentService) {
        this.taskService = taskService;
        this.commentService = commentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new task")
    public TaskDto create(@Valid @RequestBody final TaskCreateDto request) {
        return taskService.create(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a task by ID")
    public TaskDto getById(
            @Parameter(description = "Task ID") @PathVariable final UUID id) {
        return taskService.getById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a task")
    public TaskDto update(
            @Parameter(description = "Task ID") @PathVariable final UUID id,
            @Valid @RequestBody final TaskUpdateDto request) {
        return taskService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a task")
    public void delete(
            @Parameter(description = "Task ID") @PathVariable final UUID id) {
        taskService.delete(id);
    }

    @GetMapping
    @Operation(summary = "List tasks with optional filters")
    public Page<TaskDto> list(
            @Parameter(description = "Filter by status") @RequestParam(required = false) final TaskStatus status,
            @Parameter(description = "Filter by tag IDs") @RequestParam(required = false) final List<UUID> tagIds,
            @PageableDefault(size = 20, sort = "dueDate") final Pageable pageable) {
        return taskService.list(status, tagIds, pageable);
    }

    @GetMapping("/{id}/comments")
    @Operation(summary = "List comments for a task")
    public Page<CommentDto> listComments(
            @Parameter(description = "Task ID") @PathVariable final UUID id,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) final Pageable pageable) {
        return commentService.listByTask(id, pageable);
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a comment to a task")
    public CommentDto addComment(
            @Parameter(description = "Task ID") @PathVariable final UUID id,
            @Valid @RequestBody final CommentCreateDto request) {
        return commentService.addToTask(id, request);
    }
}
