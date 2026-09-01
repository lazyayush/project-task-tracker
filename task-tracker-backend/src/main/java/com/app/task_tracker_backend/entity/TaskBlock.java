package com.app.task_tracker_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "task_blocks",
        uniqueConstraints = @UniqueConstraint(columnNames = {"blocking_task_id", "blocked_task_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The task that must finish first.
    @ManyToOne
    @JoinColumn(name = "blocking_task_id", nullable = false)
    private Task blockingTask;

    // The task that cannot move to Done until blockingTask is Done.
    @ManyToOne
    @JoinColumn(name = "blocked_task_id", nullable = false)
    private Task blockedTask;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}