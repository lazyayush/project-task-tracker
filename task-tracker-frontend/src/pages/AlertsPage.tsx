import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { fetchAlerts, dismissAlert } from '../api/alerts';
import { type Task } from '../api/tasks';
import { StatusBadge, PriorityBadge } from '../components/StatusBadge';
import { TaskDetailModal } from '../components/TaskDetailModal';
import { ApiError } from '../api/client';

export function AlertsPage() {
  const { user } = useAuth();
  const [alerts, setAlerts] = useState<Task[] | null>(null);
  const [selectedTaskId, setSelectedTaskId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  function loadAlerts() {
    fetchAlerts().then(setAlerts).catch(() => setError('Could not load alerts.'));
  }

  useEffect(loadAlerts, []);

  async function handleDismiss(taskId: number) {
    setError(null);
    try {
      await dismissAlert(taskId);
      loadAlerts();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not dismiss alert');
    }
  }

  return (
    <div className="flex flex-col gap-6 max-w-4xl">
      <div>
        <h1 className="font-display text-2xl font-semibold text-ink">Alerts</h1>
        <p className="text-sm text-ink-soft mt-1">
          {user?.role === 'MANAGER'
            ? 'Overdue tasks across every project you can see.'
            : 'Overdue tasks in your projects.'}
        </p>
      </div>

      {error && <p className="text-danger text-sm">{error}</p>}

      {!alerts ? (
        <p className="text-ink-soft">Loading…</p>
      ) : alerts.length === 0 ? (
        <p className="text-ink-soft">Nothing overdue right now.</p>
      ) : (
        <div className="bg-white border border-line rounded-lg divide-y divide-line">
          {alerts.map((task) => {
            const isAssignedToMe = task.assigneeEmails.includes(user?.email ?? '');
            return (
              <div key={task.id} className="p-4 flex items-center justify-between gap-3">
                <div
                  className="min-w-0 cursor-pointer flex-1"
                  onClick={() => setSelectedTaskId(task.id)}
                >
                  <p className="text-sm font-medium text-ink truncate">{task.title}</p>
                  <div className="flex items-center gap-2 mt-1">
                    <StatusBadge status={task.status} />
                    <PriorityBadge priority={task.priority} />
                    {task.dueDate && (
                      <span className="text-xs text-amber font-medium">
                        Was due {new Date(task.dueDate).toLocaleDateString()}
                      </span>
                    )}
                  </div>
                </div>
                {isAssignedToMe ? (
                  <button
                    onClick={() => handleDismiss(task.id)}
                    className="text-sm font-medium text-ink-soft hover:text-ink border border-line rounded-lg px-3 py-1.5 shrink-0"
                  >
                    Dismiss
                  </button>
                ) : (
                  <span className="text-xs text-ink-soft/60 shrink-0">Not yours to dismiss</span>
                )}
              </div>
            );
          })}
        </div>
      )}

      {selectedTaskId && (
        <TaskDetailModal
          taskId={selectedTaskId}
          onClose={() => setSelectedTaskId(null)}
          onTaskChanged={loadAlerts}
        />
      )}
    </div>
  );
}