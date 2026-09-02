package com.app.task_tracker_backend.service;

import com.app.task_tracker_backend.entity.User;
import com.app.task_tracker_backend.dto.BulkActionRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskBulkActionExecutor {

    private final TaskService taskService;

    public TaskBulkActionExecutor(TaskService taskService) {
        this.taskService = taskService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyOne(Long taskId, BulkActionRequest.BulkActionType actionType,
                          Object newValue, User currentUser) {
        switch (actionType) {
            case STATUS_CHANGE -> taskService.transitionStatus(taskId, (com.app.task_tracker_backend.entity.TaskStatus) newValue, currentUser);
            case ASSIGNEE_CHANGE -> taskService.replaceAssignee(taskId, (String) newValue, currentUser);
            case DUE_DATE_CHANGE -> taskService.updateDueDateOnly(taskId, (java.time.Instant) newValue, currentUser);
        }
    }
}