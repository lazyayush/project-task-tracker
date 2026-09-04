import { useEffect, useState } from 'react';
import { fetchMyTasks, type Task } from '../api/tasks';
import { StatusBadge, PriorityBadge } from '../components/StatusBadge';
import { TaskDetailModal } from '../components/TaskDetailModal';

export function MyTasksPage() {
  const [tasks, setTasks] = useState<Task[] | null>(null);
  const [selectedTaskId, setSelectedTaskId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  function loadTasks() {
    fetchMyTasks().then(setTasks).catch(() => setError('Could not load your tasks.'));
  }

  useEffect(loadTasks, []);

  return (
    <div className="flex flex-col gap-6 max-w-4xl">
      <h1 className="font-display text-2xl font-semibold text-ink">My tasks</h1>

      {error && <p className="text-danger text-sm">{error}</p>}

      {!tasks ? (
        <p className="text-ink-soft">Loading…</p>
      ) : tasks.length === 0 ? (
        <p className="text-ink-soft">Nothing assigned to you right now.</p>
      ) : (
        <div className="bg-white border border-line rounded-lg divide-y divide-line">
          {tasks.map((task) => (
            <div
              key={task.id}
              onClick={() => setSelectedTaskId(task.id)}
              className="p-4 flex items-center justify-between gap-3 cursor-pointer hover:bg-ink/[0.02]"
            >
              <div className="min-w-0">
                <p className="text-sm font-medium text-ink truncate">{task.title}</p>
                <div className="flex items-center gap-2 mt-1">
                  <StatusBadge status={task.status} />
                  <PriorityBadge priority={task.priority} />
                  {task.dueDate && (
                    <span className="text-xs text-ink-soft">
                      Due {new Date(task.dueDate).toLocaleDateString()}
                    </span>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {selectedTaskId && (
        <TaskDetailModal
          taskId={selectedTaskId}
          onClose={() => setSelectedTaskId(null)}
          onTaskChanged={loadTasks}
        />
      )}
    </div>
  );
}