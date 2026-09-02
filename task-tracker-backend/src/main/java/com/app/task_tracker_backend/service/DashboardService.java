package com.app.task_tracker_backend.service;

import com.app.task_tracker_backend.entity.Task;
import com.app.task_tracker_backend.entity.TaskAssignee;
import com.app.task_tracker_backend.entity.TaskStatus;
import com.app.task_tracker_backend.entity.User;
import com.app.task_tracker_backend.repositories.ProjectMemberRepository;
import com.app.task_tracker_backend.repositories.TaskAssigneeRepository;
import com.app.task_tracker_backend.repositories.TaskRepository;
import com.app.task_tracker_backend.dto.DashboardResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final TaskRepository taskRepository;
    private final TaskAssigneeRepository taskAssigneeRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public DashboardService(
            TaskRepository taskRepository,
            TaskAssigneeRepository taskAssigneeRepository,
            ProjectMemberRepository projectMemberRepository
    ) {
        this.taskRepository = taskRepository;
        this.taskAssigneeRepository = taskAssigneeRepository;
        this.projectMemberRepository = projectMemberRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse build(User currentUser) {
        List<Task> visibleTasks = getVisibleTasks(currentUser);

        Instant now = Instant.now();

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(7);
        Instant weekStartInstant = weekStart.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant weekEndInstant = weekEnd.atStartOfDay(ZoneOffset.UTC).toInstant();

        long openTasks = visibleTasks.stream().filter(t -> t.getStatus() != TaskStatus.DONE).count();

        long overdueTasks = visibleTasks.stream()
                .filter(t -> t.getStatus() != TaskStatus.DONE)
                .filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(now))
                .count();

        long dueThisWeek = visibleTasks.stream()
                .filter(t -> t.getStatus() != TaskStatus.DONE)
                .filter(t -> t.getDueDate() != null
                        && !t.getDueDate().isBefore(weekStartInstant)
                        && t.getDueDate().isBefore(weekEndInstant))
                .count();

        long completedThisWeek = visibleTasks.stream()
                .filter(t -> t.getCompletedAt() != null
                        && !t.getCompletedAt().isBefore(weekStartInstant)
                        && t.getCompletedAt().isBefore(weekEndInstant))
                .count();

        Map<String, Long> byStatus = new HashMap<>();
        for (TaskStatus status : TaskStatus.values()) {
            byStatus.put(status.name(), visibleTasks.stream().filter(t -> t.getStatus() == status).count());
        }

        Map<String, Long> byAssignee = buildAssigneeBreakdown(visibleTasks);

        List<DashboardResponse.WeeklyCompletion> weeklyTrend = buildWeeklyTrend(visibleTasks, weekStart);

        return new DashboardResponse(openTasks, overdueTasks, dueThisWeek, completedThisWeek, byStatus, byAssignee, weeklyTrend);
    }

    private List<Task> getVisibleTasks(User currentUser) {
        boolean isManager = currentUser.getRole().name().equals("MANAGER");

        if (isManager) {
            return taskRepository.findAll().stream()
                    .filter(t -> !t.getProject().isArchived())
                    .toList();
        }

        List<Long> visibleProjectIds = projectMemberRepository.findByUserId(currentUser.getId())
                .stream()
                .map(pm -> pm.getProject().getId())
                .toList();

        return taskRepository.findAll().stream()
                .filter(t -> visibleProjectIds.contains(t.getProject().getId()))
                .filter(t -> !t.getProject().isArchived())
                .toList();
    }

    private Map<String, Long> buildAssigneeBreakdown(List<Task> tasks) {
        Map<String, Long> counts = new HashMap<>();

        for (Task task : tasks) {
            if (task.getStatus() == TaskStatus.DONE) {
                continue; // breakdown reflects current OPEN workload, not historical totals
            }

            List<TaskAssignee> assignees = taskAssigneeRepository.findByTaskId(task.getId());

            if (assignees.isEmpty()) {
                counts.merge("Unassigned", 1L, Long::sum);
            } else {
                for (TaskAssignee a : assignees) {
                    counts.merge(a.getUser().getEmail(), 1L, Long::sum);
                }
            }
        }

        return counts;
    }

    private List<DashboardResponse.WeeklyCompletion> buildWeeklyTrend(List<Task> tasks, LocalDate currentWeekStart) {
        List<DashboardResponse.WeeklyCompletion> trend = new ArrayList<>();

        for (int i = 7; i >= 0; i--) {
            LocalDate bucketStart = currentWeekStart.minusWeeks(i);
            LocalDate bucketEnd = bucketStart.plusDays(7);
            Instant bucketStartInstant = bucketStart.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant bucketEndInstant = bucketEnd.atStartOfDay(ZoneOffset.UTC).toInstant();

            long count = tasks.stream()
                    .filter(t -> t.getCompletedAt() != null
                            && !t.getCompletedAt().isBefore(bucketStartInstant)
                            && t.getCompletedAt().isBefore(bucketEndInstant))
                    .count();

            trend.add(new DashboardResponse.WeeklyCompletion(bucketStart.toString(), count));
        }

        return trend;
    }
}