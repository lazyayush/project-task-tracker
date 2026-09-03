import type { TaskStatus, Priority } from '../api/tasks';

const STATUS_CONFIG: Record<TaskStatus, { label: string; color: 'ink-soft' | 'amber' | 'success' }> = {
  BACKLOG: { label: 'Backlog', color: 'ink-soft' },
  IN_PROGRESS: { label: 'In Progress', color: 'ink-soft' },
  IN_REVIEW: { label: 'In Review', color: 'ink-soft' },
  DONE: { label: 'Done', color: 'success' },
  BLOCKED: { label: 'Blocked', color: 'amber' },
};

const PRIORITY_CONFIG: Record<Priority, { label: string; color: 'ink-soft' | 'amber' | 'success' }> = {
  LOW: { label: 'Low', color: 'ink-soft' },
  MEDIUM: { label: 'Medium', color: 'ink-soft' },
  HIGH: { label: 'High', color: 'amber' },
};

export function StatusBadge({ status }: { status: TaskStatus }) {
  const { label, color } = STATUS_CONFIG[status];
  return <Badge color={color}>{label}</Badge>;
}

export function PriorityBadge({ priority }: { priority: Priority }) {
  const { label, color } = PRIORITY_CONFIG[priority];
  return <Badge color={color}>{label}</Badge>;
}

import { Badge } from './Badge';