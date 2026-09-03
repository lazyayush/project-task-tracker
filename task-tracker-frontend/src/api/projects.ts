import { apiRequest } from './client';

export interface Project {
  id: number;
  key: string;
  name: string;
  description: string | null;
  ownerEmail: string;
  archived: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateProjectRequest {
  key: string;
  name: string;
  description: string | null;
  ownerEmail: string;
}

export function fetchProjects(includeArchived: boolean): Promise<Project[]> {
  return apiRequest<Project[]>('/api/projects', { params: { includeArchived } });
}

export function createProject(request: CreateProjectRequest): Promise<Project> {
  return apiRequest<Project>('/api/projects', { method: 'POST', body: request });
}

export function archiveProject(id: number): Promise<Project> {
  return apiRequest<Project>(`/api/projects/${id}/archive`, { method: 'PATCH' });
}

export function restoreProject(id: number): Promise<Project> {
  return apiRequest<Project>(`/api/projects/${id}/restore`, { method: 'PATCH' });
}