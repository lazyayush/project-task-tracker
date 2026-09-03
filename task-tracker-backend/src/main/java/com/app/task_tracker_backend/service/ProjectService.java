package com.app.task_tracker_backend.service;

import com.app.task_tracker_backend.entity.Project;
import com.app.task_tracker_backend.entity.ProjectMember;
import com.app.task_tracker_backend.entity.User;
import com.app.task_tracker_backend.repositories.ProjectMemberRepository;
import com.app.task_tracker_backend.repositories.ProjectRepository;
import com.app.task_tracker_backend.repositories.TaskAssigneeRepository;
import com.app.task_tracker_backend.repositories.UserRepository;
import com.app.task_tracker_backend.dto.CreateProjectRequest;
import com.app.task_tracker_backend.dto.ProjectResponse;
import com.app.task_tracker_backend.dto.UpdateProjectRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final TaskAssigneeRepository taskAssigneeRepository;

    public ProjectService(
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            UserRepository userRepository, TaskAssigneeRepository taskAssigneeRepository
    ) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
        this.taskAssigneeRepository = taskAssigneeRepository;
    }

    @Transactional
    public ProjectResponse create(CreateProjectRequest request) {
        if (projectRepository.existsByKey(request.key())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Project key already in use");
        }

        User owner = userRepository.findByEmail(request.ownerEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Owner not found: " + request.ownerEmail()));

        Instant now = Instant.now();

        Project project = Project.builder()
                .key(request.key())
                .name(request.name())
                .description(request.description())
                .owner(owner)
                .archived(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        project = projectRepository.save(project);

        ProjectMember ownerMembership = ProjectMember.builder()
                .project(project)
                .user(owner)
                .joinedAt(now)
                .build();
        projectMemberRepository.save(ownerMembership);

        return toResponse(project);
    }

    @Transactional
    public ProjectResponse update(Long projectId, UpdateProjectRequest request) {
        Project project = getOrThrow(projectId);

        project.setName(request.name());
        project.setDescription(request.description());
        project.setUpdatedAt(Instant.now());

        return toResponse(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse archive(Long projectId) {
        Project project = getOrThrow(projectId);
        project.setArchived(true);
        project.setUpdatedAt(Instant.now());
        return toResponse(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse restore(Long projectId) {
        Project project = getOrThrow(projectId);
        project.setArchived(false);
        project.setUpdatedAt(Instant.now());
        return toResponse(projectRepository.save(project));
    }

    @Transactional
    public void addMember(Long projectId, String userEmail) {
        Project project = getOrThrow(projectId);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userEmail));

        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already a member of this project");
        }

        ProjectMember membership = ProjectMember.builder()
                .project(project)
                .user(user)
                .joinedAt(Instant.now())
                .build();

        projectMemberRepository.save(membership);
    }

    @Transactional
    public void removeMember(Long projectId, String userEmail) {
        Project project = getOrThrow(projectId);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userEmail));

        if (project.getOwner().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot remove the project owner from its own membership");
        }

        projectMemberRepository.deleteByProjectIdAndUserId(projectId, user.getId());
        taskAssigneeRepository.deleteByUser_IdAndTask_Project_Id(user.getId(), projectId);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listVisibleTo(User currentUser, boolean includeArchivedRequested) {
        boolean isManager = currentUser.getRole().name().equals("MANAGER");

        boolean includeArchived = isManager && includeArchivedRequested;

        List<Project> projects;

        if (isManager) {
            projects = includeArchived
                    ? projectRepository.findAll()
                    : projectRepository.findAll().stream().filter(p -> !p.isArchived()).toList();
        } else {
            List<Long> memberProjectIds = projectMemberRepository.findByUserId(currentUser.getId())
                    .stream()
                    .map(pm -> pm.getProject().getId())
                    .toList();

            projects = projectRepository.findAllById(memberProjectIds)
                    .stream()
                    .filter(p -> includeArchived || !p.isArchived())
                    .toList();
        }

        return projects.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<String> listMembers(Long projectId, User currentUser) {
        Project project = getOrThrow(projectId);

        boolean isManager = currentUser.getRole().name().equals("MANAGER");
        boolean isMember = projectMemberRepository.existsByProjectIdAndUserId(projectId, currentUser.getId());

        if (!isManager && !isMember) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }

        return projectMemberRepository.findByProjectId(projectId).stream()
                .map(pm -> pm.getUser().getEmail())
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getVisible(Long projectId, User currentUser) {
        Project project = getOrThrow(projectId);

        boolean isManager = currentUser.getRole().name().equals("MANAGER");
        boolean isMember = projectMemberRepository.existsByProjectIdAndUserId(projectId, currentUser.getId());

        if (!isManager && !isMember) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }

        return toResponse(project);
    }

    private Project getOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    private ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getKey(),
                project.getName(),
                project.getDescription(),
                project.getOwner().getEmail(),
                project.isArchived(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}