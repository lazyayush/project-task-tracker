package com.app.task_tracker_backend.repositories;

import com.app.task_tracker_backend.entity.AlertDismissal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertDismissalRepository extends JpaRepository<AlertDismissal, Long> {

    boolean existsByTaskIdAndUserId(Long taskId, Long userId);

    void deleteByTaskId(Long taskId);

}