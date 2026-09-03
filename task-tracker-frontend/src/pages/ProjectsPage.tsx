import { useEffect, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { fetchProjects, createProject, type Project } from '../api/projects';
import { Badge } from '../components/Badge';
import { ApiError } from '../api/client';

export function ProjectsPage() {
  const { user } = useAuth();
  const isManager = user?.role === 'MANAGER';

  const [projects, setProjects] = useState<Project[] | null>(null);
  const [includeArchived, setIncludeArchived] = useState(false);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function loadProjects() {
    fetchProjects(includeArchived)
      .then(setProjects)
      .catch(() => setError('Could not load projects.'));
  }

  useEffect(() => {
    loadProjects();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [includeArchived]);

  return (
    <div className="flex flex-col gap-6 max-w-5xl">
      <div className="flex items-center justify-between">
        <h1 className="font-display text-2xl font-semibold text-ink">Projects</h1>
        {isManager && (
          <button
            onClick={() => setShowCreateForm((v) => !v)}
            className="bg-primary hover:bg-primary-dark text-white font-medium text-sm rounded-lg px-4 py-2 transition-colors"
          >
            {showCreateForm ? 'Cancel' : 'New project'}
          </button>
        )}
      </div>

      {isManager && (
        <label className="flex items-center gap-2 text-sm text-ink-soft w-fit">
          <input
            type="checkbox"
            checked={includeArchived}
            onChange={(e) => setIncludeArchived(e.target.checked)}
            className="accent-primary"
          />
          Include archived
        </label>
      )}

      {showCreateForm && (
        <CreateProjectForm
          onCreated={() => {
            setShowCreateForm(false);
            loadProjects();
          }}
        />
      )}

      {error && <p className="text-danger text-sm">{error}</p>}

      {!projects ? (
        <p className="text-ink-soft">Loading projects…</p>
      ) : projects.length === 0 ? (
        <p className="text-ink-soft">
          {isManager ? 'No projects yet — create one to get started.' : "You're not on any projects yet."}
        </p>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {projects.map((project) => (
            <Link
              key={project.id}
              to={`/projects/${project.id}`}
              className="bg-white border border-line rounded-lg p-5 hover:border-primary/40 transition-colors"
            >
              <div className="flex items-start justify-between">
                <div>
                  <span className="text-xs font-mono text-ink-soft">{project.key}</span>
                  <h2 className="font-display font-semibold text-ink mt-0.5">{project.name}</h2>
                </div>
                {project.archived && <Badge color="ink-soft">Archived</Badge>}
              </div>
              {project.description && (
                <p className="text-sm text-ink-soft mt-2 line-clamp-2">{project.description}</p>
              )}
              <p className="text-xs text-ink-soft/70 mt-3">Owner: {project.ownerEmail}</p>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}

function CreateProjectForm({ onCreated }: { onCreated: () => void }) {
  const [key, setKey] = useState('');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [ownerEmail, setOwnerEmail] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await createProject({ key: key.toUpperCase(), name, description: description || null, ownerEmail });
      onCreated();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not create project');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="bg-white border border-line rounded-lg p-5 flex flex-col gap-4">
      {error && <p className="text-danger text-sm">{error}</p>}

      <div className="grid grid-cols-2 gap-4">
        <div className="flex flex-col gap-1.5">
          <label className="text-sm font-medium text-ink-soft">Key</label>
          <input
            value={key}
            onChange={(e) => setKey(e.target.value)}
            placeholder="PROJ"
            pattern="[A-Za-z0-9]{2,10}"
            title="2-10 letters/digits"
            className="border border-line rounded-lg px-3 py-2 text-ink focus:outline-none focus:ring-2 focus:ring-primary/30 focus:border-primary"
            required
          />
        </div>
        <div className="flex flex-col gap-1.5">
          <label className="text-sm font-medium text-ink-soft">Owner email</label>
          <input
            type="email"
            value={ownerEmail}
            onChange={(e) => setOwnerEmail(e.target.value)}
            className="border border-line rounded-lg px-3 py-2 text-ink focus:outline-none focus:ring-2 focus:ring-primary/30 focus:border-primary"
            required
          />
        </div>
      </div>

      <div className="flex flex-col gap-1.5">
        <label className="text-sm font-medium text-ink-soft">Name</label>
        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          className="border border-line rounded-lg px-3 py-2 text-ink focus:outline-none focus:ring-2 focus:ring-primary/30 focus:border-primary"
          required
        />
      </div>

      <div className="flex flex-col gap-1.5">
        <label className="text-sm font-medium text-ink-soft">Description</label>
        <textarea
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          rows={2}
          className="border border-line rounded-lg px-3 py-2 text-ink focus:outline-none focus:ring-2 focus:ring-primary/30 focus:border-primary resize-none"
        />
      </div>

      <button
        type="submit"
        disabled={submitting}
        className="bg-primary hover:bg-primary-dark text-white font-medium rounded-lg px-4 py-2 w-fit transition-colors disabled:opacity-50"
      >
        {submitting ? 'Creating…' : 'Create project'}
      </button>
    </form>
  );
}