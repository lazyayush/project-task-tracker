package com.app.task_tracker_backend.repositories;

import com.app.task_tracker_backend.entity.TaskBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskBlockRepository extends JpaRepository<TaskBlock, Long> {

    List<TaskBlock> findByBlockedTaskId(Long blockedTaskId);

    List<TaskBlock> findByBlockingTaskId(Long blockingTaskId);

    boolean existsByBlockingTaskIdAndBlockedTaskId(Long blockingTaskId, Long blockedTaskId);
}