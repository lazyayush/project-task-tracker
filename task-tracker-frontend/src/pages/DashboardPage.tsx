import { useEffect, useState } from 'react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts';
import { fetchDashboard, type DashboardData } from '../api/dashboard';
import { StatCard } from '../components/StatCard';

const STATUS_LABELS: Record<string, string> = {
  BACKLOG: 'Backlog',
  IN_PROGRESS: 'In Progress',
  IN_REVIEW: 'In Review',
  DONE: 'Done',
  BLOCKED: 'Blocked',
};

export function DashboardPage() {
  const [data, setData] = useState<DashboardData | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchDashboard()
      .then(setData)
      .catch(() => setError('Could not load dashboard data.'));
  }, []);

  if (error) {
    return <p className="text-danger">{error}</p>;
  }

  if (!data) {
    return <p className="text-ink-soft">Loading dashboard…</p>;
  }

  const chartData = data.completionsLast8Weeks.map((w) => ({
    week: new Date(w.weekStart).toLocaleDateString(undefined, { month: 'short', day: 'numeric' }),
    completed: w.count,
  }));

  const assigneeEntries = Object.entries(data.byAssignee).sort(([, a], [, b]) => b - a);

  return (
    <div className="flex flex-col gap-6 max-w-10xl">
      <h1 className="font-display text-2xl font-semibold text-ink">Dashboard</h1>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <StatCard label="Open tasks" value={data.openTasks} />
        <StatCard label="Overdue" value={data.overdueTasks} accent="amber" />
        <StatCard label="Due this week" value={data.dueThisWeek} accent="amber" />
        <StatCard label="Completed this week" value={data.completedThisWeek} accent="success" />
      </div>

      <div className="bg-white border border-line rounded-lg p-5">
        <h2 className="font-display text-lg font-semibold text-ink mb-4">Completions, last 8 weeks</h2>
        <ResponsiveContainer width="100%" height={220}>
          <BarChart data={chartData}>
            <CartesianGrid strokeDasharray="3 3" stroke="#E4E1D9" vertical={false} />
            <XAxis dataKey="week" tick={{ fontSize: 12, fill: '#55534E' }} axisLine={{ stroke: '#E4E1D9' }} />
            <YAxis allowDecimals={false} tick={{ fontSize: 12, fill: '#55534E' }} axisLine={{ stroke: '#E4E1D9' }} />
            <Tooltip
              contentStyle={{ borderRadius: 8, border: '1px solid #E4E1D9', fontSize: 13 }}
              cursor={{ fill: '#22345C', opacity: 0.05 }}
            />
            <Bar dataKey="completed" fill="#22345C" radius={[4, 4, 0, 0]} />
          </BarChart>
        </ResponsiveContainer>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="bg-white border border-line rounded-lg p-5">
          <h2 className="font-display text-lg font-semibold text-ink mb-3">By status</h2>
          <div className="flex flex-col gap-2">
            {Object.entries(data.byStatus).map(([status, count]) => (
              <div key={status} className="flex items-center justify-between text-sm">
                <span className="text-ink-soft">{STATUS_LABELS[status] ?? status}</span>
                <span className="font-medium text-ink">{count}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="bg-white border border-line rounded-lg p-5">
          <h2 className="font-display text-lg font-semibold text-ink mb-3">Open tasks by assignee</h2>
          <div className="flex flex-col gap-2">
            {assigneeEntries.length === 0 ? (
              <p className="text-sm text-ink-soft">No open tasks assigned.</p>
            ) : (
              assigneeEntries.map(([email, count]) => (
                <div key={email} className="flex items-center justify-between text-sm">
                  <span className="text-ink-soft truncate">{email}</span>
                  <span className="font-medium text-ink">{count}</span>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
}