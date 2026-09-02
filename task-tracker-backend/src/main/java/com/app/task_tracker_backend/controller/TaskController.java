package com.app.task_tracker_backend.controller;

import com.app.task_tracker_backend.dto.*;
import com.app.task_tracker_backend.entity.Priority;
import com.app.task_tracker_backend.entity.TaskStatus;
import com.app.task_tracker_backend.entity.User;
import com.app.task_tracker_backend.repositories.UserRepository;
import com.app.task_tracker_backend.service.TaskSearchCriteria;
import com.app.task_tracker_backend.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
public class TaskController {

    private final TaskService taskService;
    private final UserRepository userRepository;

    public TaskController(TaskService taskService, UserRepository userRepository) {
        this.taskService = taskService;
        this.userRepository = userRepository;
    }

    @PostMapping("/api/projects/{projectId}/tasks")
    public ResponseEntity<TaskResponse> create(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateTaskRequest request,
            Authentication authentication
    ) {
        User currentUser = currentUser(authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(projectId, request, currentUser));
    }

    @GetMapping("/api/projects/{projectId}/tasks")
    public List<TaskResponse> listByProject(@PathVariable Long projectId, Authentication authentication) {
        return taskService.listByProject(projectId, currentUser(authentication));
    }

    @PutMapping("/api/tasks/{taskId}")
    public TaskResponse update(
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskRequest request,
            Authentication authentication
    ) {
        return taskService.update(taskId, request, currentUser(authentication));
    }

    @DeleteMapping("/api/tasks/{taskId}")
    public ResponseEntity<Void> delete(@PathVariable Long taskId, Authentication authentication) {
        taskService.delete(taskId, currentUser(authentication));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/tasks/{taskId}/blockers")
    public ResponseEntity<Void> addBlocker(
            @PathVariable Long taskId,
            @Valid @RequestBody AddBlockerRequest request,
            Authentication authentication
    ) {
        taskService.addBlocker(taskId, request.blockingTaskId(), currentUser(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/api/tasks/{taskId}/blockers/{blockingTaskId}")
    public ResponseEntity<Void> removeBlocker(
            @PathVariable Long taskId,
            @PathVariable Long blockingTaskId,
            Authentication authentication
    ) {
        taskService.removeBlocker(taskId, blockingTaskId, currentUser(authentication));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/tasks/{taskId}/status")
    public TaskResponse transitionStatus(
            @PathVariable Long taskId,
            @RequestBody Map<String, String> body,
            Authentication authentication
    ) {
        TaskStatus newStatus = TaskStatus.valueOf(body.get("status"));
        return taskService.transitionStatus(taskId, newStatus, currentUser(authentication));
    }

    @GetMapping("/api/tasks/{taskId}/legal-transitions")
    public Set<TaskStatus> legalTransitions(@PathVariable Long taskId, Authentication authentication) {
        return taskService.getLegalTransitions(taskId, currentUser(authentication));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    @PostMapping("/api/tasks/{taskId}/assignees")
    public ResponseEntity<Void> assign(
            @PathVariable Long taskId,
            @Valid @RequestBody AddMemberRequest request,
            Authentication authentication
    ) {
        taskService.assign(taskId, request.userEmail(), currentUser(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/api/tasks/{taskId}/assignees")
    public ResponseEntity<Void> unassign(
            @PathVariable Long taskId,
            @Valid @RequestBody AddMemberRequest request,
            Authentication authentication
    ) {
        taskService.unassign(taskId, request.userEmail(), currentUser(authentication));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/me/tasks")
    public List<TaskResponse> myTasks(Authentication authentication) {
        return taskService.myTasks(currentUser(authentication));
    }

    @GetMapping("/api/tasks/search")
    public PagedResponse<TaskResponse> search(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) String assigneeEmail,
            @RequestParam(required = false) Priority priority,
            @RequestParam(defaultValue = "false") boolean overdueOnly,
            @RequestParam(defaultValue = "dueDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        TaskSearchCriteria criteria = new TaskSearchCriteria(
                searchTerm, projectId, status, assigneeEmail, priority,
                overdueOnly, sortBy, sortDirection, page, size
        );
        return taskService.search(criteria, currentUser(authentication));
    }

    @PostMapping("/api/tasks/bulk-action")
    public BulkActionResponse bulkAction(@Valid @RequestBody BulkActionRequest request, Authentication authentication) {
        return taskService.applyBulkAction(request, currentUser(authentication));
    }

    @GetMapping(value = "/api/tasks/export", produces = "text/csv")
    public ResponseEntity<String> export(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) String assigneeEmail,
            @RequestParam(required = false) Priority priority,
            @RequestParam(defaultValue = "false") boolean overdueOnly,
            Authentication authentication
    ) {
        TaskSearchCriteria criteria = new TaskSearchCriteria(
                searchTerm, projectId, status, assigneeEmail, priority,
                overdueOnly, null, null, 0, Integer.MAX_VALUE
        );
        String csv = taskService.exportFilteredAsCsv(criteria, currentUser(authentication));

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=tasks_export.csv")
                .body(csv);
    }
}