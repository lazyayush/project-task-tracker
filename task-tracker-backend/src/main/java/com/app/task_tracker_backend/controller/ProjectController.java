package com.app.task_tracker_backend.controller;

import com.app.task_tracker_backend.entity.User;
import com.app.task_tracker_backend.repositories.UserRepository;
import com.app.task_tracker_backend.service.ProjectService;
import com.app.task_tracker_backend.dto.AddMemberRequest;
import com.app.task_tracker_backend.dto.CreateProjectRequest;
import com.app.task_tracker_backend.dto.ProjectResponse;
import com.app.task_tracker_backend.dto.UpdateProjectRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final UserRepository userRepository;

    public ProjectController(ProjectService projectService, UserRepository userRepository) {
        this.projectService = projectService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.create(request));
    }

    @PutMapping("/{id}")
    public ProjectResponse update(@PathVariable Long id, @Valid @RequestBody UpdateProjectRequest request) {
        return projectService.update(id, request);
    }

    @PatchMapping("/{id}/archive")
    public ProjectResponse archive(@PathVariable Long id) {
        return projectService.archive(id);
    }

    @PatchMapping("/{id}/restore")
    public ProjectResponse restore(@PathVariable Long id) {
        return projectService.restore(id);
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<Void> addMember(@PathVariable Long id, @Valid @RequestBody AddMemberRequest request) {
        projectService.addMember(id, request.userEmail());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/members")
    public ResponseEntity<Void> removeMember(@PathVariable Long id, @Valid @RequestBody AddMemberRequest request) {
        projectService.removeMember(id, request.userEmail());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/members")
    public List<String> listMembers(@PathVariable Long id, Authentication authentication) {
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return projectService.listMembers(id, currentUser);
    }

    @GetMapping("/{id}")
    public ProjectResponse get(@PathVariable Long id, Authentication authentication) {
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return projectService.getVisible(id, currentUser);
    }

    @GetMapping
    public List<ProjectResponse> list(
            @RequestParam(defaultValue = "false") boolean includeArchived,
            Authentication authentication
    ) {
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        return projectService.listVisibleTo(currentUser, includeArchived);
    }
}