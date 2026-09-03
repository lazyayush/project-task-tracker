import { apiRequest } from './client';

export interface DashboardData {
  openTasks: number;
  overdueTasks: number;
  dueThisWeek: number;
  completedThisWeek: number;
  byStatus: Record<string, number>;
  byAssignee: Record<string, number>;
  completionsLast8Weeks: { weekStart: string; count: number }[];
}

export function fetchDashboard(): Promise<DashboardData> {
  return apiRequest<DashboardData>('/api/dashboard');
}