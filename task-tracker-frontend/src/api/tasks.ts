import { apiRequest } from './client';

export type TaskStatus = 'BACKLOG' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE' | 'BLOCKED';
export type Priority = 'LOW' | 'MEDIUM' | 'HIGH';

export interface Task {
  id: number;
  projectId: number;
  title: string;
  description: string | null;
  priority: Priority;
  dueDate: string | null;
  status: TaskStatus;
  blockedFromStatus: TaskStatus | null;
  blockingTaskIds: number[];
  assigneeEmails: string[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateTaskRequest {
  title: string;
  description: string | null;
  priority: Priority;
  dueDate: string | null;
}

export function fetchProjectTasks(projectId: number): Promise<Task[]> {
  return apiRequest<Task[]>(`/api/projects/${projectId}/tasks`);
}

export function createTask(projectId: number, request: CreateTaskRequest): Promise<Task> {
  return apiRequest<Task>(`/api/projects/${projectId}/tasks`, { method: 'POST', body: request });
}

export function fetchLegalTransitions(taskId: number): Promise<TaskStatus[]> {
  return apiRequest<TaskStatus[]>(`/api/tasks/${taskId}/legal-transitions`);
}

export function transitionTaskStatus(taskId: number, status: TaskStatus): Promise<Task> {
  return apiRequest<Task>(`/api/tasks/${taskId}/status`, { method: 'PATCH', body: { status } });
}