import { useEffect, useState } from 'react';
import { fetchLegalTransitions, transitionTaskStatus, type Task, type TaskStatus } from '../api/tasks';
import { ApiError } from '../api/client';

const STATUS_LABELS: Record<TaskStatus, string> = {
  BACKLOG: 'Backlog',
  IN_PROGRESS: 'In Progress',
  IN_REVIEW: 'In Review',
  DONE: 'Done',
  BLOCKED: 'Blocked',
};

interface Props {
  task: Task;
  onChanged: (updated: Task) => void;
}

export function TaskStatusControl({ task, onChanged }: Props) {
  const [legalTransitions, setLegalTransitions] = useState<TaskStatus[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchLegalTransitions(task.id).then(setLegalTransitions).catch(() => setLegalTransitions([]));
  }, [task.id, task.status]);

  async function handleChange(newStatus: TaskStatus) {
    setError(null);
    try {
      const updated = await transitionTaskStatus(task.id, newStatus);
      onChanged(updated);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not update status');
    }
  }

  return (
    <div className="flex items-center gap-2">
      <select
        value=""
        onChange={(e) => e.target.value && handleChange(e.target.value as TaskStatus)}
        disabled={legalTransitions.length === 0}
        className="text-sm border border-line rounded-lg px-2 py-1 text-ink-soft focus:outline-none focus:ring-2 focus:ring-primary/30"
      >
        <option value="">Move to…</option>
        {legalTransitions.map((status) => (
          <option key={status} value={status}>{STATUS_LABELS[status]}</option>
        ))}
      </select>
      {error && <span className="text-danger text-xs">{error}</span>}
    </div>
  );
}