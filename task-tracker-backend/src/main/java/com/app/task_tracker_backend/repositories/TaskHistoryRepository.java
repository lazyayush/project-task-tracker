package com.app.task_tracker_backend.repositories;

import com.app.task_tracker_backend.entity.TaskHistoryEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskHistoryRepository extends JpaRepository<TaskHistoryEntry, Long> {

    List<TaskHistoryEntry> findByTaskIdOrderByCreatedAtAsc(Long taskId);
}