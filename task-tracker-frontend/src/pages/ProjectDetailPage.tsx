import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  fetchProject, fetchProjectMembers, addProjectMember, removeProjectMember,
  archiveProject, restoreProject, type Project,
} from '../api/projects';
import { Badge } from '../components/Badge';
import { ApiError } from '../api/client';
import { fetchProjectTasks, type Task } from '../api/tasks';
import { StatusBadge, PriorityBadge } from '../components/StatusBadge';
import { TaskStatusControl } from '../components/TaskStatusControl';
import { CreateTaskForm } from '../components/CreateTaskForm';
import { TaskDetailModal } from '../components/TaskDetailModal';

export function ProjectDetailPage() {
  const { id } = useParams<{ id: string }>();
  const projectId = Number(id);
  const navigate = useNavigate();
  const { user } = useAuth();
  const isManager = user?.role === 'MANAGER';

  const [project, setProject] = useState<Project | null>(null);
  const [members, setMembers] = useState<string[] | null>(null);
  const [newMemberEmail, setNewMemberEmail] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const [tasks, setTasks] = useState<Task[] | null>(null);
  const [showTaskForm, setShowTaskForm] = useState(false);
  const [selectedTaskId, setSelectedTaskId] = useState<number | null>(null);

  function loadTasks() {
    fetchProjectTasks(projectId)
      .then(setTasks)
      .catch(() => setTasks([]));
  }

  useEffect(loadTasks, [projectId]);

  function handleTaskStatusChanged(updated: Task) {
    setTasks(
      (prev) =>
        prev?.map((t) => (t.id === updated.id ? updated : t)) ?? null
    );
  }

  function loadAll() {
    fetchProject(projectId)
      .then(setProject)
      .catch(() => setError('Project not found.'));

    fetchProjectMembers(projectId)
      .then(setMembers)
      .catch(() => setMembers([]));
  }

  useEffect(loadAll, [projectId]);

  async function handleAddMember(e: React.FormEvent) {
    e.preventDefault();
    setActionError(null);

    try {
      await addProjectMember(projectId, newMemberEmail);
      setNewMemberEmail('');
      loadAll();
    } catch (err) {
      setActionError(
        err instanceof ApiError ? err.message : 'Could not add member'
      );
    }
  }

  async function handleRemoveMember(email: string) {
    setActionError(null);

    try {
      await removeProjectMember(projectId, email);
      loadAll();
    } catch (err) {
      setActionError(
        err instanceof ApiError ? err.message : 'Could not remove member'
      );
    }
  }

  async function handleArchiveToggle() {
    if (!project) return;

    try {
      const updated = project.archived
        ? await restoreProject(project.id)
        : await archiveProject(project.id);

      setProject(updated);
    } catch (err) {
      setActionError(
        err instanceof ApiError
          ? err.message
          : 'Could not update project status'
      );
    }
  }

  if (error) return <p className="text-danger">{error}</p>;
  if (!project) return <p className="text-ink-soft">Loading project…</p>;

  return (
    <div className="flex flex-col gap-6 max-w-3xl">
      <button
        onClick={() => navigate('/projects')}
        className="text-sm text-ink-soft hover:text-ink w-fit"
      >
        ← Back to projects
      </button>

      <div className="flex items-start justify-between">
        <div>
          <span className="text-xs font-mono text-ink-soft">
            {project.key}
          </span>

          <h1 className="font-display text-2xl font-semibold text-ink mt-0.5">
            {project.name}
          </h1>

          <p className="text-sm text-ink-soft mt-1">
            Owner: {project.ownerEmail}
          </p>
        </div>

        <div className="flex items-center gap-2">
          {project.archived && (
            <Badge color="ink-soft">Archived</Badge>
          )}

          {isManager && (
            <button
              onClick={handleArchiveToggle}
              className="text-sm font-medium text-ink-soft hover:text-ink border border-line rounded-lg px-3 py-1.5 transition-colors"
            >
              {project.archived ? 'Restore' : 'Archive'}
            </button>
          )}
        </div>
      </div>

      {project.description && (
        <p className="text-ink-soft">{project.description}</p>
      )}

      {actionError && (
        <p className="text-danger text-sm">{actionError}</p>
      )}

      <div className="bg-white border border-line rounded-lg p-5">
        <h2 className="font-display text-lg font-semibold text-ink mb-3">
          Members
        </h2>

        {!members ? (
          <p className="text-sm text-ink-soft">Loading members…</p>
        ) : (
          <div className="flex flex-col gap-2 mb-4">
            {members.map((email) => (
              <div
                key={email}
                className="flex items-center justify-between text-sm py-1.5"
              >
                <span className="text-ink">
                  {email}{' '}
                  {email === project.ownerEmail && (
                    <span className="text-ink-soft/60">(owner)</span>
                  )}
                </span>

                {isManager && email !== project.ownerEmail && (
                  <button
                    onClick={() => handleRemoveMember(email)}
                    className="text-ink-soft hover:text-danger text-xs font-medium"
                  >
                    Remove
                  </button>
                )}
              </div>
            ))}
          </div>
        )}

        {isManager && (
          <form onSubmit={handleAddMember} className="flex gap-2">
            <input
              type="email"
              value={newMemberEmail}
              onChange={(e) => setNewMemberEmail(e.target.value)}
              placeholder="user@email.com"
              className="flex-1 border border-line rounded-lg px-3 py-1.5 text-sm text-ink focus:outline-none focus:ring-2 focus:ring-primary/30 focus:border-primary"
              required
            />

            <button
              type="submit"
              className="bg-primary hover:bg-primary-dark text-white text-sm font-medium rounded-lg px-4 py-1.5 transition-colors"
            >
              Add
            </button>
          </form>
        )}
      </div>

      {/* Tasks */}
      <div className="bg-white border border-line rounded-lg p-5">
        <div className="flex items-center justify-between mb-3">
          <h2 className="font-display text-lg font-semibold text-ink">
            Tasks
          </h2>

          {isManager && (
            <button
              onClick={() => setShowTaskForm((v) => !v)}
              className="text-sm font-medium text-primary hover:underline"
            >
              {showTaskForm ? 'Cancel' : '+ Add task'}
            </button>
          )}
        </div>

        {showTaskForm && (
          <div className="mb-4">
            <CreateTaskForm
              projectId={projectId}
              onCreated={() => {
                setShowTaskForm(false);
                loadTasks();
              }}
            />
          </div>
        )}

        {!tasks ? (
          <p className="text-sm text-ink-soft">
            Loading tasks…
          </p>
        ) : tasks.length === 0 ? (
          <p className="text-sm text-ink-soft">
            No tasks yet.
          </p>
        ) : (
          <div className="flex flex-col divide-y divide-line">
            {tasks.map((task) => (
              <div
                key={task.id}
                onClick={() => setSelectedTaskId(task.id)}
                className="py-3 flex items-center justify-between gap-3 cursor-pointer hover:bg-ink/[0.02] transition-colors"
              >
                <div className="min-w-0">
                  <p className="text-sm font-medium text-ink truncate">
                    {task.title}
                  </p>

                  <div className="flex items-center gap-2 mt-1">
                    <StatusBadge status={task.status} />

                    <PriorityBadge priority={task.priority} />

                    {task.dueDate && (
                      <span className="text-xs text-ink-soft">
                        Due{' '}
                        {new Date(
                          task.dueDate
                        ).toLocaleDateString()}
                      </span>
                    )}
                  </div>
                </div>

                <div onClick={(e) => e.stopPropagation()}>
                  <TaskStatusControl
                    task={task}
                    onChanged={handleTaskStatusChanged}
                  />
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

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

