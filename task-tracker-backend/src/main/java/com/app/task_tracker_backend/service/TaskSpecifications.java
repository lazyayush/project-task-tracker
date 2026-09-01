package com.app.task_tracker_backend.service;

import com.app.task_tracker_backend.entity.Priority;
import com.app.task_tracker_backend.entity.Project;
import com.app.task_tracker_backend.entity.Task;
import com.app.task_tracker_backend.entity.TaskAssignee;
import com.app.task_tracker_backend.entity.TaskStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Collection;

public final class TaskSpecifications {

    private TaskSpecifications() {
    }

    public static Specification<Task> projectIdIn(Collection<Long> projectIds) {
        return (root, query, cb) -> root.get("project").get("id").in(projectIds);
    }

    public static Specification<Task> projectIdEquals(Long projectId) {
        return (root, query, cb) -> cb.equal(root.get("project").get("id"), projectId);
    }

    public static Specification<Task> projectNotArchived() {
        return (root, query, cb) -> cb.isFalse(root.get("project").get("archived"));
    }

    public static Specification<Task> statusEquals(TaskStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Task> priorityEquals(Priority priority) {
        return (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    public static Specification<Task> titleOrDescriptionContains(String term) {
        String pattern = "%" + term.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(cb.coalesce(root.get("description"), "")), pattern)
        );
    }

    public static Specification<Task> isOverdue() {
        return (root, query, cb) -> cb.and(
                cb.isNotNull(root.get("dueDate")),
                cb.lessThan(root.get("dueDate"), cb.literal(Instant.now())),
                cb.notEqual(root.get("status"), TaskStatus.DONE)
        );
    }

    public static Specification<Task> assigneeEmailEquals(String email) {
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            var assigneeRoot = subquery.from(TaskAssignee.class);
            subquery.select(assigneeRoot.get("task").get("id"));
            subquery.where(
                    cb.equal(assigneeRoot.get("task"), root),
                    cb.equal(assigneeRoot.get("user").get("email"), email)
            );
            return cb.exists(subquery);
        };
    }

    public static Specification<Task> orderByPriorityRank(boolean descending) {
        return (root, query, cb) -> {
            Expression<Integer> rank = cb.<Integer>selectCase()
                    .when(cb.equal(root.get("priority"), Priority.LOW), 1)
                    .when(cb.equal(root.get("priority"), Priority.MEDIUM), 2)
                    .when(cb.equal(root.get("priority"), Priority.HIGH), 3)
                    .otherwise(0);

            query.orderBy(descending ? cb.desc(rank) : cb.asc(rank));
            return cb.conjunction();
        };
    }
}