package com.app.task_tracker_backend.repositories;

import com.app.task_tracker_backend.entity.TaskAssignee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskAssigneeRepository extends JpaRepository<TaskAssignee, Long> {

    List<TaskAssignee> findByTaskId(Long taskId);

    List<TaskAssignee> findByUserId(Long userId);

    boolean existsByTaskIdAndUserId(Long taskId, Long userId);

    void deleteByTaskIdAndUserId(Long taskId, Long userId);

    // Used by the project-membership-removal cascade — deletes every
    // assignment this user holds on any task within one specific project.
    void deleteByUser_IdAndTask_Project_Id(Long userId, Long projectId);
}