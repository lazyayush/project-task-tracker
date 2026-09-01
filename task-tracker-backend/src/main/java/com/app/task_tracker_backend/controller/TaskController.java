package com.app.task_tracker_backend.controller;

import com.app.task_tracker_backend.entity.User;
import com.app.task_tracker_backend.repositories.UserRepository;
import com.app.task_tracker_backend.service.TaskService;
import com.app.task_tracker_backend.dto.AddBlockerRequest;
import com.app.task_tracker_backend.dto.CreateTaskRequest;
import com.app.task_tracker_backend.dto.TaskResponse;
import com.app.task_tracker_backend.dto.UpdateTaskRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

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

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}