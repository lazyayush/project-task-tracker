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

export interface UpdateTaskRequest {
  title: string;
  description: string | null;
  priority: Priority;
  dueDate: string | null;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface SearchParams {
  searchTerm?: string;
  projectId?: number;
  status?: TaskStatus;
  assigneeEmail?: string;
  priority?: Priority;
  overdueOnly?: boolean;
  sortBy?: 'dueDate' | 'priority' | 'updatedAt';
  sortDirection?: 'asc' | 'desc';
  page?: number;
  size?: number;
}

export function searchTasks(params: SearchParams): Promise<PagedResponse<Task>> {
  return apiRequest<PagedResponse<Task>>('/api/tasks/search', { params: params as Record<string, string | number | boolean | undefined> });
}

export type BulkActionType = 'STATUS_CHANGE' | 'ASSIGNEE_CHANGE' | 'DUE_DATE_CHANGE';

export interface BulkActionResult {
  taskId: number;
  success: boolean;
  message: string;
}

export interface BulkActionResponse {
  totalRequested: number;
  succeeded: number;
  failed: number;
  results: BulkActionResult[];
}

export function applyBulkAction(
  taskIds: number[],
  actionType: BulkActionType,
  value: TaskStatus | string | string | null
): Promise<BulkActionResponse> {
  const body: Record<string, unknown> = { taskIds, actionType };
  if (actionType === 'STATUS_CHANGE') body.newStatus = value;
  if (actionType === 'ASSIGNEE_CHANGE') body.newAssigneeEmail = value;
  if (actionType === 'DUE_DATE_CHANGE') body.newDueDate = value;

  return apiRequest<BulkActionResponse>('/api/tasks/bulk-action', { method: 'POST', body });
}

export async function exportTasksCsv(params: Omit<SearchParams, 'sortBy' | 'sortDirection' | 'page' | 'size'>): Promise<void> {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined) query.append(key, String(value));
  });

  const token = localStorage.getItem('token');
  const response = await fetch(`${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/api/tasks/export?${query}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });

  if (!response.ok) throw new Error('Export failed');

  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = 'tasks_export.csv';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
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

export function fetchTask(taskId: number): Promise<Task> {
  return apiRequest<Task>(`/api/tasks/${taskId}`);
}

export function addBlocker(taskId: number, blockingTaskId: number): Promise<void> {
  return apiRequest<void>(`/api/tasks/${taskId}/blockers`, { method: 'POST', body: { blockingTaskId } });
}

export function removeBlocker(taskId: number, blockingTaskId: number): Promise<void> {
  return apiRequest<void>(`/api/tasks/${taskId}/blockers/${blockingTaskId}`, { method: 'DELETE' });
}

export function assignUser(taskId: number, userEmail: string): Promise<void> {
  return apiRequest<void>(`/api/tasks/${taskId}/assignees`, { method: 'POST', body: { userEmail } });
}

export function unassignUser(taskId: number, userEmail: string): Promise<void> {
  return apiRequest<void>(`/api/tasks/${taskId}/assignees`, { method: 'DELETE', body: { userEmail } });
}

export function fetchMyTasks(): Promise<Task[]> {
  return apiRequest<Task[]>('/api/me/tasks');
}

export function updateTask(taskId: number, request: UpdateTaskRequest): Promise<Task> {
  return apiRequest<Task>(`/api/tasks/${taskId}`, { method: 'PUT', body: request });
}