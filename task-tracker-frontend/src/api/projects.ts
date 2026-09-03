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

export function fetchProject(id: number): Promise<Project> {
  return apiRequest<Project>(`/api/projects/${id}`);
}

export function fetchProjectMembers(id: number): Promise<string[]> {
  return apiRequest<string[]>(`/api/projects/${id}/members`);
}

export function addProjectMember(id: number, userEmail: string): Promise<void> {
  return apiRequest<void>(`/api/projects/${id}/members`, { method: 'POST', body: { userEmail } });
}

export function removeProjectMember(id: number, userEmail: string): Promise<void> {
  return apiRequest<void>(`/api/projects/${id}/members`, { method: 'DELETE', body: { userEmail } });
}

export function updateProject(id: number, name: string, description: string | null): Promise<Project> {
  return apiRequest<Project>(`/api/projects/${id}`, { method: 'PUT', body: { name, description } });
}