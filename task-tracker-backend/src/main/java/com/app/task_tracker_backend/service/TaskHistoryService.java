package com.app.task_tracker_backend.service;

import com.app.task_tracker_backend.entity.HistoryEventType;
import com.app.task_tracker_backend.entity.Task;
import com.app.task_tracker_backend.entity.TaskHistoryEntry;
import com.app.task_tracker_backend.entity.User;
import com.app.task_tracker_backend.repositories.TaskHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

@Service
public class TaskHistoryService {

    private final TaskHistoryRepository taskHistoryRepository;

    public TaskHistoryService(TaskHistoryRepository taskHistoryRepository) {
        this.taskHistoryRepository = taskHistoryRepository;
    }

    public void recordCreated(Task task, User actor) {
        save(task, actor, HistoryEventType.CREATED, null, null, null, null);
    }

    public void recordFieldChange(Task task, User actor, String fieldName, Object oldValue, Object newValue) {
        String oldStr = oldValue != null ? oldValue.toString() : null;
        String newStr = newValue != null ? newValue.toString() : null;

        if (Objects.equals(oldStr, newStr)) {
            return;
        }

        save(task, actor, HistoryEventType.FIELD_CHANGED, fieldName, oldStr, newStr, null);
    }

    public void recordAssigned(Task task, User actor, String assigneeEmail) {
        save(task, actor, HistoryEventType.ASSIGNED, "assignee", null, assigneeEmail, null);
    }

    public void recordUnassigned(Task task, User actor, String assigneeEmail) {
        save(task, actor, HistoryEventType.UNASSIGNED, "assignee", assigneeEmail, null, null);
    }

    public void recordComment(Task task, User actor, String commentText) {
        save(task, actor, HistoryEventType.COMMENT, null, null, null, commentText);
    }

    private void save(Task task, User actor, HistoryEventType type, String field, String oldVal, String newVal, String comment) {
        TaskHistoryEntry entry = TaskHistoryEntry.builder()
                .task(task)
                .actor(actor)
                .eventType(type)
                .fieldName(field)
                .oldValue(oldVal)
                .newValue(newVal)
                .commentText(comment)
                .createdAt(Instant.now())
                .build();

        taskHistoryRepository.save(entry);
    }
}