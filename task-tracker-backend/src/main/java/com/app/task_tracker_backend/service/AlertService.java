package com.app.task_tracker_backend.service;

import com.app.task_tracker_backend.entity.AlertDismissal;
import com.app.task_tracker_backend.entity.Task;
import com.app.task_tracker_backend.entity.TaskStatus;
import com.app.task_tracker_backend.entity.User;
import com.app.task_tracker_backend.repositories.AlertDismissalRepository;
import com.app.task_tracker_backend.repositories.ProjectMemberRepository;
import com.app.task_tracker_backend.repositories.TaskAssigneeRepository;
import com.app.task_tracker_backend.repositories.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class AlertService {

    private final TaskRepository taskRepository;
    private final TaskAssigneeRepository taskAssigneeRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final AlertDismissalRepository alertDismissalRepository;

    public AlertService(
            TaskRepository taskRepository,
            TaskAssigneeRepository taskAssigneeRepository,
            ProjectMemberRepository projectMemberRepository,
            AlertDismissalRepository alertDismissalRepository
    ) {
        this.taskRepository = taskRepository;
        this.taskAssigneeRepository = taskAssigneeRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.alertDismissalRepository = alertDismissalRepository;
    }

    @Transactional(readOnly = true)
    public List<Task> getActiveAlerts(User currentUser) {
        Instant now = Instant.now();
        boolean isManager = currentUser.getRole().name().equals("MANAGER");

        List<Task> visibleTasks;

        if (isManager) {
            visibleTasks = taskRepository.findAll().stream()
                    .filter(t -> !t.getProject().isArchived())
                    .toList();
        } else {
            List<Long> visibleProjectIds = projectMemberRepository.findByUserId(currentUser.getId())
                    .stream()
                    .map(pm -> pm.getProject().getId())
                    .toList();

            visibleTasks = taskRepository.findAll().stream()
                    .filter(t -> visibleProjectIds.contains(t.getProject().getId()))
                    .filter(t -> !t.getProject().isArchived())
                    .toList();
        }

        return visibleTasks.stream()
                .filter(task -> task.getStatus() != TaskStatus.DONE)
                .filter(task -> task.getDueDate() != null && task.getDueDate().isBefore(now))
                .filter(task -> !alertDismissalRepository.existsByTaskIdAndUserId(task.getId(), currentUser.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public long getActiveAlertCount(User currentUser) {
        return getActiveAlerts(currentUser).size();
    }

    @Transactional
    public void dismiss(Long taskId, User currentUser) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        boolean isAssigned = taskAssigneeRepository.existsByTaskIdAndUserId(taskId, currentUser.getId());

        if (!isAssigned) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only dismiss alerts for tasks assigned to you");
        }

        AlertDismissal dismissal = AlertDismissal.builder()
                .task(task)
                .user(currentUser)
                .dismissedAt(Instant.now())
                .build();

        alertDismissalRepository.save(dismissal);
    }
}