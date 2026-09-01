package com.app.task_tracker_backend.service;

import com.app.task_tracker_backend.entity.Project;
import com.app.task_tracker_backend.entity.Task;
import com.app.task_tracker_backend.entity.TaskBlock;
import com.app.task_tracker_backend.entity.TaskStatus;
import com.app.task_tracker_backend.entity.User;
import com.app.task_tracker_backend.repositories.ProjectMemberRepository;
import com.app.task_tracker_backend.repositories.ProjectRepository;
import com.app.task_tracker_backend.repositories.TaskBlockRepository;
import com.app.task_tracker_backend.repositories.TaskRepository;
import com.app.task_tracker_backend.dto.CreateTaskRequest;
import com.app.task_tracker_backend.dto.TaskResponse;
import com.app.task_tracker_backend.dto.UpdateTaskRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskBlockRepository taskBlockRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public TaskService(
            TaskRepository taskRepository,
            TaskBlockRepository taskBlockRepository,
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository
    ) {
        this.taskRepository = taskRepository;
        this.taskBlockRepository = taskBlockRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
    }

    @Transactional
    public TaskResponse create(Long projectId, CreateTaskRequest request, User currentUser) {
        Project project = getProjectOrThrow(projectId);
        assertManagerIsProjectMember(currentUser, project);

        Instant now = Instant.now();

        Task task = Task.builder()
                .project(project)
                .title(request.title())
                .description(request.description())
                .priority(request.priority())
                .dueDate(request.dueDate())
                .status(TaskStatus.BACKLOG)
                .createdAt(now)
                .updatedAt(now)
                .build();

        task = taskRepository.save(task);
        return toResponse(task);
    }

    @Transactional
    public TaskResponse update(Long taskId, UpdateTaskRequest request, User currentUser) {
        Task task = getTaskOrThrow(taskId);
        assertManagerIsProjectMember(currentUser, task.getProject());

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setPriority(request.priority());
        task.setDueDate(request.dueDate());
        task.setUpdatedAt(Instant.now());

        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public void delete(Long taskId, User currentUser) {
        Task task = getTaskOrThrow(taskId);
        assertManagerIsProjectMember(currentUser, task.getProject());

        taskRepository.delete(task);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listByProject(Long projectId, User currentUser) {
        Project project = getProjectOrThrow(projectId);
        assertCanView(currentUser, project);

        return taskRepository.findByProjectId(projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void addBlocker(Long taskId, Long blockingTaskId, User currentUser) {
        Task blockedTask = getTaskOrThrow(taskId);
        Task blockingTask = getTaskOrThrow(blockingTaskId);

        assertManagerIsProjectMember(currentUser, blockedTask.getProject());

        if (!blockedTask.getProject().getId().equals(blockingTask.getProject().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A task can only be blocked by a task in the same project");
        }

        if (blockedTask.getId().equals(blockingTask.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A task cannot block itself");
        }

        if (taskBlockRepository.existsByBlockingTaskIdAndBlockedTaskId(blockingTaskId, taskId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This blocking relationship already exists");
        }

        TaskBlock block = TaskBlock.builder()
                .blockingTask(blockingTask)
                .blockedTask(blockedTask)
                .createdAt(Instant.now())
                .build();

        taskBlockRepository.save(block);
    }

    @Transactional
    public void removeBlocker(Long taskId, Long blockingTaskId, User currentUser) {
        Task blockedTask = getTaskOrThrow(taskId);
        assertManagerIsProjectMember(currentUser, blockedTask.getProject());

        taskBlockRepository.findByBlockedTaskId(taskId).stream()
                .filter(b -> b.getBlockingTask().getId().equals(blockingTaskId))
                .findFirst()
                .ifPresentOrElse(
                        taskBlockRepository::delete,
                        () -> { throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such blocking relationship"); }
                );
    }

    private void assertManagerIsProjectMember(User user, Project project) {
        boolean isManager = user.getRole().name().equals("MANAGER");
        if (!isManager) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only managers can modify tasks");
        }

        boolean isMember = projectMemberRepository.existsByProjectIdAndUserId(project.getId(), user.getId());
        if (!isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You must be a member of this project to modify its tasks");
        }
    }

    private void assertCanView(User user, Project project) {
        boolean isManager = user.getRole().name().equals("MANAGER");

        if (isManager) {
            return;
        }

        boolean isMember = projectMemberRepository.existsByProjectIdAndUserId(project.getId(), user.getId());

        if (!isMember || project.isArchived()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }
    }

    private Project getProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    private Task getTaskOrThrow(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    private TaskResponse toResponse(Task task) {
        List<Long> blockerIds = taskBlockRepository.findByBlockedTaskId(task.getId()).stream()
                .map(b -> b.getBlockingTask().getId())
                .toList();

        return new TaskResponse(
                task.getId(),
                task.getProject().getId(),
                task.getTitle(),
                task.getDescription(),
                task.getPriority(),
                task.getDueDate(),
                task.getStatus(),
                task.getBlockedFromStatus(),
                blockerIds,
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}