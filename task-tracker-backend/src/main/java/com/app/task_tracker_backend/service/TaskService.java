package com.app.task_tracker_backend.service;

import com.app.task_tracker_backend.dto.PagedResponse;
import com.app.task_tracker_backend.entity.*;
import com.app.task_tracker_backend.repositories.*;
import com.app.task_tracker_backend.dto.CreateTaskRequest;
import com.app.task_tracker_backend.dto.TaskResponse;
import com.app.task_tracker_backend.dto.UpdateTaskRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskBlockRepository taskBlockRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskAssigneeRepository taskAssigneeRepository;
    private final UserRepository userRepository;

    public TaskService(
            TaskRepository taskRepository,
            TaskBlockRepository taskBlockRepository,
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository, TaskAssigneeRepository taskAssigneeRepository, UserRepository userRepository
    ) {
        this.taskRepository = taskRepository;
        this.taskBlockRepository = taskBlockRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.taskAssigneeRepository = taskAssigneeRepository;
        this.userRepository = userRepository;
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

    @Transactional
    public TaskResponse transitionStatus(Long taskId, TaskStatus newStatus, User currentUser) {
        Task task = getTaskOrThrow(taskId);
        assertProjectMember(currentUser, task.getProject()); // any member, not manager-only — see Decision log

        TaskStatus current = task.getStatus();

        if (newStatus == TaskStatus.BLOCKED) {
            if (current != TaskStatus.IN_PROGRESS && current != TaskStatus.IN_REVIEW) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot block a task from " + current + " — only In Progress or In Review can be blocked");
            }
            task.setBlockedFromStatus(current);
            task.setStatus(TaskStatus.BLOCKED);

        } else if (current == TaskStatus.BLOCKED) {
            // The only legal move FROM Blocked is unblocking, back to blockedFromStatus.
            if (!newStatus.equals(task.getBlockedFromStatus())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A blocked task can only return to " + task.getBlockedFromStatus() + ", not " + newStatus);
            }
            task.setStatus(newStatus);
            task.setBlockedFromStatus(null);

        } else if (newStatus == TaskStatus.DONE) {
            if (!TaskTransitionRules.allowedFrom(current).contains(newStatus)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot move from " + current + " directly to Done");
            }
            assertNoUnfinishedBlockers(task);
            task.setStatus(newStatus);

        } else {
            if (!TaskTransitionRules.allowedFrom(current).contains(newStatus)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot move from " + current + " to " + newStatus);
            }
            task.setStatus(newStatus);
        }

        task.setUpdatedAt(Instant.now());
        return toResponse(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public Set<TaskStatus> getLegalTransitions(Long taskId, User currentUser) {
        Task task = getTaskOrThrow(taskId);
        assertProjectMember(currentUser, task.getProject());

        TaskStatus current = task.getStatus();

        if (current == TaskStatus.BLOCKED) {
            return Set.of(task.getBlockedFromStatus());
        }

        Set<TaskStatus> candidates = TaskTransitionRules.allowedFrom(current);

        // Done is atcually legal right now if there are no unfinished blockers —
        // this is the one case where "legal transition" depends on more than the
        // status graph alone, so it can't be answered by the static map by itself.
        if (candidates.contains(TaskStatus.DONE) && hasUnfinishedBlockers(task)) {
            return candidates.stream().filter(s -> s != TaskStatus.DONE).collect(Collectors.toSet());
        }

        return candidates;
    }

    private void assertNoUnfinishedBlockers(Task task) {
        if (hasUnfinishedBlockers(task)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot move to Done — this task has an unfinished blocking task");
        }
    }

    private boolean hasUnfinishedBlockers(Task task) {
        return taskBlockRepository.findByBlockedTaskId(task.getId()).stream()
                .map(TaskBlock::getBlockingTask)
                .anyMatch(blocker -> blocker.getStatus() != TaskStatus.DONE);
    }

    private void assertProjectMember(User user, Project project) {
        boolean isManager = user.getRole().name().equals("MANAGER");
        boolean isMember = projectMemberRepository.existsByProjectIdAndUserId(project.getId(), user.getId());

        if (!isManager && !isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You must be a member of this project");
        }
    }

    @Transactional
    public void assign(Long taskId, String userEmail, User currentUser) {
        Task task = getTaskOrThrow(taskId);
        assertCanManageAssignment(currentUser, task.getProject());

        User userToAssign = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userEmail));

        boolean isMember = projectMemberRepository.existsByProjectIdAndUserId(task.getProject().getId(), userToAssign.getId());
        if (!isMember) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only members of this project may be assigned to its tasks");
        }

        if (taskAssigneeRepository.existsByTaskIdAndUserId(taskId, userToAssign.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already assigned to this task");
        }

        TaskAssignee assignee = TaskAssignee.builder()
                .task(task)
                .user(userToAssign)
                .assignedAt(Instant.now())
                .build();

        taskAssigneeRepository.save(assignee);
    }

    @Transactional
    public void unassign(Long taskId, String userEmail, User currentUser) {
        Task task = getTaskOrThrow(taskId);
        assertCanManageAssignment(currentUser, task.getProject());

        User userToUnassign = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userEmail));

        taskAssigneeRepository.deleteByTaskIdAndUserId(taskId, userToUnassign.getId());
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> myTasks(User currentUser) {
        return taskAssigneeRepository.findByUserId(currentUser.getId()).stream()
                .map(TaskAssignee::getTask)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<TaskResponse> search(TaskSearchCriteria criteria, User currentUser) {
        boolean isManager = currentUser.getRole().name().equals("MANAGER");

        Specification<Task> spec = Specification.unrestricted();

        if (!isManager) {
            List<Long> visibleProjectIds = projectMemberRepository.findByUserId(currentUser.getId())
                    .stream()
                    .map(pm -> pm.getProject().getId())
                    .toList();

            spec = spec.and(TaskSpecifications.projectIdIn(visibleProjectIds))
                    .and(TaskSpecifications.projectNotArchived());
        } else {
            spec = spec.and(TaskSpecifications.projectNotArchived());
        }

        if (criteria.projectId() != null) {
            spec = spec.and(TaskSpecifications.projectIdEquals(criteria.projectId()));
        }
        if (criteria.status() != null) {
            spec = spec.and(TaskSpecifications.statusEquals(criteria.status()));
        }
        if (criteria.priority() != null) {
            spec = spec.and(TaskSpecifications.priorityEquals(criteria.priority()));
        }
        if (criteria.searchTerm() != null && !criteria.searchTerm().isBlank()) {
            spec = spec.and(TaskSpecifications.titleOrDescriptionContains(criteria.searchTerm()));
        }
        if (criteria.assigneeEmail() != null && !criteria.assigneeEmail().isBlank()) {
            spec = spec.and(TaskSpecifications.assigneeEmailEquals(criteria.assigneeEmail()));
        }
        if (criteria.overdueOnly()) {
            spec = spec.and(TaskSpecifications.isOverdue());
        }

        int pageSize = Math.min(Math.max(criteria.size(), 1), 100); // hard cap at 100
        int pageNumber = Math.max(criteria.page(), 0);

        Pageable pageable;
        if ("priority".equals(criteria.sortBy())) {
            boolean descending = "desc".equalsIgnoreCase(criteria.sortDirection());
            spec = spec.and(TaskSpecifications.orderByPriorityRank(descending));
            pageable = PageRequest.of(pageNumber, pageSize); // ordering handled by the Specification itself
        } else {
            String sortField = "updatedAt".equals(criteria.sortBy()) ? "updatedAt" : "dueDate"; // default: dueDate
            Sort.Direction direction = "desc".equalsIgnoreCase(criteria.sortDirection())
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;
            pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, sortField));
        }

        Page<Task> results = taskRepository.findAll(spec, pageable);

        return new PagedResponse<>(
                results.getContent().stream().map(this::toResponse).toList(),
                results.getNumber(),
                results.getSize(),
                results.getTotalElements(),
                results.getTotalPages()
        );
    }

    private void assertCanManageAssignment(User user, Project project) {
        boolean isOwner = project.getOwner().getId().equals(user.getId());
        boolean isManagerMember = user.getRole().name().equals("MANAGER")
                && projectMemberRepository.existsByProjectIdAndUserId(project.getId(), user.getId());

        if (!isOwner && !isManagerMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the project owner or a project-member manager can manage assignments");
        }
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

        List<String> assigneeEmails = taskAssigneeRepository.findByTaskId(task.getId()).stream()
                .map(a -> a.getUser().getEmail())
                .toList();

        return new TaskResponse(
                task.getId(), task.getProject().getId(), task.getTitle(), task.getDescription(),
                task.getPriority(), task.getDueDate(), task.getStatus(), task.getBlockedFromStatus(),
                blockerIds, assigneeEmails, task.getCreatedAt(), task.getUpdatedAt()
        );
    }
}