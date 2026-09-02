package com.app.task_tracker_backend.controller;

import com.app.task_tracker_backend.entity.User;
import com.app.task_tracker_backend.repositories.UserRepository;
import com.app.task_tracker_backend.service.AlertService;
import com.app.task_tracker_backend.service.TaskService;
import com.app.task_tracker_backend.dto.TaskResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;
    private final TaskService taskService;
    private final UserRepository userRepository;

    public AlertController(AlertService alertService, TaskService taskService, UserRepository userRepository) {
        this.alertService = alertService;
        this.taskService = taskService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<TaskResponse> getAlerts(Authentication authentication) {
        User currentUser = currentUser(authentication);

        return alertService.getActiveAlerts(currentUser).stream()
                .map(taskService::toResponse)
                .toList();
    }

    @GetMapping("/count")
    public Map<String, Long> getAlertCount(Authentication authentication) {
        User currentUser = currentUser(authentication);
        return Map.of("count", alertService.getActiveAlertCount(currentUser));
    }

    @PostMapping("/{taskId}/dismiss")
    public ResponseEntity<Void> dismiss(@PathVariable Long taskId, Authentication authentication) {
        alertService.dismiss(taskId, currentUser(authentication));
        return ResponseEntity.noContent().build();
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}