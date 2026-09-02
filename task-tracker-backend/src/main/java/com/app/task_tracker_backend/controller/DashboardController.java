package com.app.task_tracker_backend.controller;

import com.app.task_tracker_backend.dto.DashboardResponse;
import com.app.task_tracker_backend.entity.User;
import com.app.task_tracker_backend.repositories.UserRepository;
import com.app.task_tracker_backend.service.DashboardService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class DashboardController {

    private final UserRepository userRepository;
    private final DashboardService dashboardService;

    public DashboardController(UserRepository userRepository, DashboardService dashboardService) {
        this.userRepository = userRepository;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/api/dashboard")
    public DashboardResponse dashboard(Authentication authentication) {
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return dashboardService.build(currentUser);
    }
}
