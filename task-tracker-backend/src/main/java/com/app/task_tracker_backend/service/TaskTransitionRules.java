package com.app.task_tracker_backend.service;

import com.app.task_tracker_backend.entity.TaskStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class TaskTransitionRules {

    private static final Map<TaskStatus, Set<TaskStatus>> TRANSITIONS = new EnumMap<>(TaskStatus.class);

    static {
        TRANSITIONS.put(TaskStatus.BACKLOG, EnumSet.of(TaskStatus.IN_PROGRESS));
        TRANSITIONS.put(TaskStatus.IN_PROGRESS, EnumSet.of(TaskStatus.IN_REVIEW, TaskStatus.BLOCKED));
        TRANSITIONS.put(TaskStatus.IN_REVIEW, EnumSet.of(TaskStatus.DONE, TaskStatus.BLOCKED));
        TRANSITIONS.put(TaskStatus.DONE, EnumSet.of(TaskStatus.IN_PROGRESS)); // reopen
        TRANSITIONS.put(TaskStatus.BLOCKED, EnumSet.noneOf(TaskStatus.class)); // handled specially — only unblock, back to blockedFromStatus
    }

    private TaskTransitionRules() {
    }

    public static Set<TaskStatus> allowedFrom(TaskStatus current) {
        return TRANSITIONS.getOrDefault(current, EnumSet.noneOf(TaskStatus.class));
    }
}