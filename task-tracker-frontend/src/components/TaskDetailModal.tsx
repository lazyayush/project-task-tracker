import { useEffect, useState, type FormEvent } from 'react';
import { useAuth } from '../context/AuthContext';
import {
  fetchTask, fetchProjectTasks, addBlocker, removeBlocker, assignUser, unassignUser,
  type Task,
} from '../api/tasks';
import { fetchProject } from '../api/projects';
import { StatusBadge, PriorityBadge } from './StatusBadge';
import { TaskStatusControl } from './TaskStatusControl';
import { TaskHistoryPanel } from './TaskHistoryPanel';
import { ApiError } from '../api/client';

interface Props {
  taskId: number;
  onClose: () => void;
  onTaskChanged: () => void;
}

export function TaskDetailModal({ taskId, onClose, onTaskChanged }: Props) {
  const { user } = useAuth();
  const isManager = user?.role === 'MANAGER';

  const [task, setTask] = useState<Task | null>(null);
  const [projectOwnerEmail, setProjectOwnerEmail] = useState<string | null>(null);
  const [projectTasks, setProjectTasks] = useState<Task[]>([]);
  const [newAssigneeEmail, setNewAssigneeEmail] = useState('');
  const [selectedBlockerId, setSelectedBlockerId] = useState('');
  const [error, setError] = useState<string | null>(null);

  function reload() {
    fetchTask(taskId).then(setTask).catch(() => setError('Could not load task.'));
  }

  useEffect(() => {
    reload();
  }, [taskId]);

  useEffect(() => {
    if (!task) return;
    fetchProject(task.projectId).then((p) => setProjectOwnerEmail(p.ownerEmail)).catch(() => {});
    fetchProjectTasks(task.projectId).then(setProjectTasks).catch(() => setProjectTasks([]));
  }, [task?.projectId]);

  const isOwner = user?.email === projectOwnerEmail;
  const canManageAssignment = isManager || isOwner;

  function handleStatusChanged(updated: Task) {
    setTask(updated);
    onTaskChanged();
  }

  async function handleAddAssignee(e: FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      await assignUser(taskId, newAssigneeEmail);
      setNewAssigneeEmail('');
      reload();
      onTaskChanged();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not assign user');
    }
  }

  async function handleRemoveAssignee(email: string) {
    setError(null);
    try {
      await unassignUser(taskId, email);
      reload();
      onTaskChanged();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not unassign user');
    }
  }

  async function handleAddBlocker(e: FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      await addBlocker(taskId, Number(selectedBlockerId));
      setSelectedBlockerId('');
      reload();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not add blocker');
    }
  }

  async function handleRemoveBlocker(blockingTaskId: number) {
    setError(null);
    try {
      await removeBlocker(taskId, blockingTaskId);
      reload();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not remove blocker');
    }
  }

  const blockerCandidates = projectTasks.filter(
    (t) => t.id !== taskId && !task?.blockingTaskIds.includes(t.id)
  );

  return (
    <div className="fixed inset-0 bg-ink/40 flex items-center justify-end z-50" onClick={onClose}>
      <div
        className="bg-paper h-full w-full max-w-md shadow-xl overflow-y-auto p-6 flex flex-col gap-6"
        onClick={(e) => e.stopPropagation()}
      >
        <button onClick={onClose} className="text-sm text-ink-soft hover:text-ink w-fit">✕ Close</button>

        {error && <p className="text-danger text-sm">{error}</p>}

        {!task ? (
          <p className="text-ink-soft">Loading…</p>
        ) : (
          <>
            <div>
              <h2 className="font-display text-xl font-semibold text-ink">{task.title}</h2>
              {task.description && <p className="text-sm text-ink-soft mt-2">{task.description}</p>}
              <div className="flex items-center gap-2 mt-3">
                <StatusBadge status={task.status} />
                <PriorityBadge priority={task.priority} />
              </div>
            </div>

            <div>
              <h3 className="text-sm font-semibold text-ink-soft mb-2">Status</h3>
              <TaskStatusControl task={task} onChanged={handleStatusChanged} />
            </div>

            <div className="bg-white border border-line rounded-lg p-4">
              <h3 className="text-sm font-semibold text-ink mb-3">Assignees</h3>
              {task.assigneeEmails.length === 0 ? (
                <p className="text-sm text-ink-soft mb-3">No one assigned.</p>
              ) : (
                <div className="flex flex-col gap-2 mb-3">
                  {task.assigneeEmails.map((email) => (
                    <div key={email} className="flex items-center justify-between text-sm">
                      <span className="text-ink">{email}</span>
                      {canManageAssignment && (
                        <button
                          onClick={() => handleRemoveAssignee(email)}
                          className="text-ink-soft hover:text-danger text-xs font-medium"
                        >
                          Remove
                        </button>
                      )}
                    </div>
                  ))}
                </div>
              )}
              {canManageAssignment && (
                <form onSubmit={handleAddAssignee} className="flex gap-2">
                  <input
                    type="email"
                    value={newAssigneeEmail}
                    onChange={(e) => setNewAssigneeEmail(e.target.value)}
                    placeholder="user@email.com"
                    className="flex-1 border border-line rounded-lg px-2 py-1.5 text-sm text-ink focus:outline-none focus:ring-2 focus:ring-primary/30"
                    required
                  />
                  <button
                    type="submit"
                    className="bg-primary hover:bg-primary-dark text-white text-xs font-medium rounded-lg px-3 py-1.5"
                  >
                    Assign
                  </button>
                </form>
              )}
            </div>

            <div className="bg-white border border-line rounded-lg p-4">
              <h3 className="text-sm font-semibold text-ink mb-3">Blocked by</h3>
              {task.blockingTaskIds.length === 0 ? (
                <p className="text-sm text-ink-soft mb-3">Nothing blocking this task.</p>
              ) : (
                <div className="flex flex-col gap-2 mb-3">
                  {task.blockingTaskIds.map((id) => {
                    const blocker = projectTasks.find((t) => t.id === id);
                    return (
                      <div key={id} className="flex items-center justify-between text-sm">
                        <span className="text-ink flex items-center gap-2">
                          {blocker?.title ?? `Task #${id}`}
                          {blocker && <StatusBadge status={blocker.status} />}
                        </span>
                        {isManager && (
                          <button
                            onClick={() => handleRemoveBlocker(id)}
                            className="text-ink-soft hover:text-danger text-xs font-medium"
                          >
                            Remove
                          </button>
                        )}
                      </div>
                    );
                  })}
                </div>
              )}
              {isManager && blockerCandidates.length > 0 && (
                <form onSubmit={handleAddBlocker} className="flex gap-2">
                  <select
                    value={selectedBlockerId}
                    onChange={(e) => setSelectedBlockerId(e.target.value)}
                    className="flex-1 border border-line rounded-lg px-2 py-1.5 text-sm text-ink bg-white focus:outline-none focus:ring-2 focus:ring-primary/30"
                    required
                  >
                    <option value="">Select a task…</option>
                    {blockerCandidates.map((t) => (
                      <option key={t.id} value={t.id}>{t.title}</option>
                    ))}
                  </select>
                  <button
                    type="submit"
                    className="bg-primary hover:bg-primary-dark text-white text-xs font-medium rounded-lg px-3 py-1.5"
                  >
                    Block
                  </button>
                </form>
              )}
            </div>

            <TaskHistoryPanel taskId={taskId} />
          </>
        )}
      </div>
    </div>
  );
}