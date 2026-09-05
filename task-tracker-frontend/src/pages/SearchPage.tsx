import { useEffect, useState } from 'react';
import { searchTasks, applyBulkAction, exportTasksCsv, type Task, type SearchParams, type BulkActionType, type BulkActionResponse } from '../api/tasks';
import { fetchProjects, type Project } from '../api/projects';
import { StatusBadge, PriorityBadge } from '../components/StatusBadge';
import { TaskDetailModal } from '../components/TaskDetailModal';
import { ApiError } from '../api/client';

const PAGE_SIZE = 20;

export function SearchPage() {
  const [filters, setFilters] = useState<SearchParams>({ page: 0, size: PAGE_SIZE, sortBy: 'dueDate', sortDirection: 'asc' });
  const [projects, setProjects] = useState<Project[]>([]);
  const [results, setResults] = useState<Task[] | null>(null);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [selectedTaskId, setSelectedTaskId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [bulkActionType, setBulkActionType] = useState<BulkActionType>('STATUS_CHANGE');
  const [bulkValue, setBulkValue] = useState('');
  const [bulkResult, setBulkResult] = useState<BulkActionResponse | null>(null);
  const [applyingBulk, setApplyingBulk] = useState(false);


  useEffect(() => {
    fetchProjects(false).then(setProjects).catch(() => setProjects([]));
  }, []);

  function runSearch() {
    searchTasks(filters)
      .then((data) => {
        setResults(data.content);
        setTotalElements(data.totalElements);
        setTotalPages(data.totalPages);
      })
      .catch(() => setError('Search failed.'));
  }

  useEffect(runSearch, [filters]);

  function updateFilter<K extends keyof SearchParams>(key: K, value: SearchParams[K]) {
    setFilters((prev) => ({ ...prev, [key]: value, page: 0 })); // any filter change resets to page 0
    setSelectedIds(new Set());
  }

  function toggleSelected(id: number) {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  }

  function toggleSelectAll() {
    if (!results) return;
    setSelectedIds((prev) => (prev.size === results.length ? new Set() : new Set(results.map((t) => t.id))));
  }

  async function handleBulkAction() {
    if (applyingBulk) return; // hard guard against double-fire even if the button click somehow slips through
    setApplyingBulk(true);
    setError(null);
    setBulkResult(null);
    try {
      let value: string | null = bulkValue;
      if (bulkActionType === 'DUE_DATE_CHANGE') {
        value = bulkValue ? new Date(bulkValue).toISOString() : null;
      }
      const result = await applyBulkAction(Array.from(selectedIds), bulkActionType, value);
      setBulkResult(result);
      setSelectedIds(new Set());
      runSearch();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Bulk action failed');
    } finally {
      setApplyingBulk(false);
    }
  }

  async function handleExport() {
    try {
      await exportTasksCsv({
        searchTerm: filters.searchTerm,
        projectId: filters.projectId,
        status: filters.status,
        assigneeEmail: filters.assigneeEmail,
        priority: filters.priority,
        overdueOnly: filters.overdueOnly,
      });
    } catch {
      setError('Export failed.');
    }
  }

  return (
    <div className="flex flex-col gap-5 max-w-6xl">
      <div className="flex items-center justify-between">
        <h1 className="font-display text-2xl font-semibold text-ink">Search tasks</h1>
        <button
          onClick={handleExport}
          className="text-sm font-medium text-ink-soft hover:text-ink border border-line rounded-lg px-3 py-1.5"
        >
          Export CSV
        </button>
      </div>

      <div className="bg-white border border-line rounded-lg p-4 flex flex-wrap gap-3 items-end">
        <div className="flex flex-col gap-1">
          <label className="text-xs font-medium text-ink-soft">Search</label>
          <input
            value={filters.searchTerm ?? ''}
            onChange={(e) => updateFilter('searchTerm', e.target.value || undefined)}
            placeholder="Title or description"
            className="border border-line rounded-lg px-3 py-1.5 text-sm text-ink w-48 focus:outline-none focus:ring-2 focus:ring-primary/30"
          />
        </div>

        <div className="flex flex-col gap-1">
          <label className="text-xs font-medium text-ink-soft">Project</label>
          <select
            value={filters.projectId ?? ''}
            onChange={(e) => updateFilter('projectId', e.target.value ? Number(e.target.value) : undefined)}
            className="border border-line rounded-lg px-3 py-1.5 text-sm text-ink bg-white focus:outline-none focus:ring-2 focus:ring-primary/30"
          >
            <option value="">All</option>
            {projects.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
          </select>
        </div>

        <div className="flex flex-col gap-1">
          <label className="text-xs font-medium text-ink-soft">Status</label>
          <select
            value={filters.status ?? ''}
            onChange={(e) => updateFilter('status', (e.target.value || undefined) as SearchParams['status'])}
            className="border border-line rounded-lg px-3 py-1.5 text-sm text-ink bg-white focus:outline-none focus:ring-2 focus:ring-primary/30"
          >
            <option value="">All</option>
            <option value="BACKLOG">Backlog</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="IN_REVIEW">In Review</option>
            <option value="DONE">Done</option>
            <option value="BLOCKED">Blocked</option>
          </select>
        </div>

        <div className="flex flex-col gap-1">
          <label className="text-xs font-medium text-ink-soft">Priority</label>
          <select
            value={filters.priority ?? ''}
            onChange={(e) => updateFilter('priority', (e.target.value || undefined) as SearchParams['priority'])}
            className="border border-line rounded-lg px-3 py-1.5 text-sm text-ink bg-white focus:outline-none focus:ring-2 focus:ring-primary/30"
          >
            <option value="">All</option>
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
          </select>
        </div>

        <div className="flex flex-col gap-1">
          <label className="text-xs font-medium text-ink-soft">Assignee</label>
          <input
            value={filters.assigneeEmail ?? ''}
            onChange={(e) => updateFilter('assigneeEmail', e.target.value || undefined)}
            placeholder="email"
            className="border border-line rounded-lg px-3 py-1.5 text-sm text-ink w-40 focus:outline-none focus:ring-2 focus:ring-primary/30"
          />
        </div>

        <label className="flex items-center gap-1.5 text-sm text-ink-soft pb-1.5">
          <input
            type="checkbox"
            checked={filters.overdueOnly ?? false}
            onChange={(e) => updateFilter('overdueOnly', e.target.checked)}
            className="accent-primary"
          />
          Overdue only
        </label>

        <div className="flex flex-col gap-1">
          <label className="text-xs font-medium text-ink-soft">Sort by</label>
          <select
            value={filters.sortBy}
            onChange={(e) => updateFilter('sortBy', e.target.value as SearchParams['sortBy'])}
            className="border border-line rounded-lg px-3 py-1.5 text-sm text-ink bg-white focus:outline-none focus:ring-2 focus:ring-primary/30"
          >
            <option value="dueDate">Due date</option>
            <option value="priority">Priority</option>
            <option value="updatedAt">Last update</option>
          </select>
        </div>
      </div>

      {error && <p className="text-danger text-sm">{error}</p>}

      {selectedIds.size > 0 && (
        <div className="bg-primary/5 border border-primary/20 rounded-lg p-4 flex flex-wrap items-end gap-3">
          <span className="text-sm font-medium text-ink">{selectedIds.size} selected</span>

          <select
            value={bulkActionType}
            onChange={(e) => { setBulkActionType(e.target.value as BulkActionType); setBulkValue(''); }}
            className="border border-line rounded-lg px-2 py-1.5 text-sm text-ink bg-white"
          >
            <option value="STATUS_CHANGE">Change status</option>
            <option value="ASSIGNEE_CHANGE">Reassign</option>
            <option value="DUE_DATE_CHANGE">Change due date</option>
          </select>

          {bulkActionType === 'STATUS_CHANGE' && (
            <select value={bulkValue} onChange={(e) => setBulkValue(e.target.value)} className="border border-line rounded-lg px-2 py-1.5 text-sm text-ink bg-white" required>
              <option value="">Select…</option>
              <option value="BACKLOG">Backlog</option>
              <option value="IN_PROGRESS">In Progress</option>
              <option value="IN_REVIEW">In Review</option>
              <option value="DONE">Done</option>
            </select>
          )}
          {bulkActionType === 'ASSIGNEE_CHANGE' && (
            <input type="email" value={bulkValue} onChange={(e) => setBulkValue(e.target.value)} placeholder="user@email.com" className="border border-line rounded-lg px-2 py-1.5 text-sm text-ink" required />
          )}
          {bulkActionType === 'DUE_DATE_CHANGE' && (
            <input type="datetime-local" value={bulkValue} onChange={(e) => setBulkValue(e.target.value)} className="border border-line rounded-lg px-2 py-1.5 text-sm text-ink" />
          )}

          <button
            onClick={handleBulkAction}
            disabled={applyingBulk}
            className="bg-primary hover:bg-primary-dark text-white text-sm font-medium rounded-lg px-4 py-1.5 disabled:opacity-50"
          >
            {applyingBulk ? 'Applying…' : 'Apply'}
          </button>
        </div>
      )}

      {bulkResult && (
        <div className="bg-white border border-line rounded-lg p-4">
          <p className="text-sm font-medium text-ink mb-2">
            {bulkResult.succeeded} succeeded, {bulkResult.failed} failed (of {bulkResult.totalRequested})
          </p>
          {bulkResult.failed > 0 && (
            <div className="flex flex-col gap-1">
              {bulkResult.results.filter((r) => !r.success).map((r) => {
                const task = results?.find((t) => t.id === r.taskId);
                return (
                  <p key={r.taskId} className="text-xs text-danger">
                    {task ? task.title : `Task #${r.taskId}`}: {r.message}
                  </p>
                );
              })}
            </div>
          )}
        </div>
      )}

      {!results ? (
        <p className="text-ink-soft">Loading…</p>
      ) : results.length === 0 ? (
        <p className="text-ink-soft">No tasks match these filters.</p>
      ) : (
        <div className="bg-white border border-line rounded-lg">
          <div className="flex items-center gap-3 p-3 border-b border-line text-xs font-medium text-ink-soft">
            <input
              type="checkbox"
              checked={selectedIds.size === results.length}
              onChange={toggleSelectAll}
              className="accent-primary"
            />
            <span>{totalElements} total matches</span>
          </div>
          <div className="divide-y divide-line">
            {results.map((task) => (
              <div key={task.id} className="p-3 flex items-center gap-3">
                <input
                  type="checkbox"
                  checked={selectedIds.has(task.id)}
                  onChange={() => toggleSelected(task.id)}
                  className="accent-primary"
                />
                <div className="min-w-0 flex-1 cursor-pointer" onClick={() => setSelectedTaskId(task.id)}>
                  <p className="text-sm font-medium text-ink truncate">{task.title}</p>
                  <div className="flex items-center gap-2 mt-1">
                    <StatusBadge status={task.status} />
                    <PriorityBadge priority={task.priority} />
                    {task.dueDate && (
                      <span className="text-xs text-ink-soft">Due {new Date(task.dueDate).toLocaleDateString()}</span>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>

          <div className="flex items-center justify-between p-3 border-t border-line text-sm">
            <button
              disabled={(filters.page ?? 0) === 0}
              onClick={() => setFilters((f) => ({ ...f, page: (f.page ?? 0) - 1 }))}
              className="text-ink-soft hover:text-ink disabled:opacity-30"
            >
              ← Previous
            </button>
            <span className="text-ink-soft">Page {(filters.page ?? 0) + 1} of {totalPages || 1}</span>
            <button
              disabled={(filters.page ?? 0) + 1 >= totalPages}
              onClick={() => setFilters((f) => ({ ...f, page: (f.page ?? 0) + 1 }))}
              className="text-ink-soft hover:text-ink disabled:opacity-30"
            >
              Next →
            </button>
          </div>
        </div>
      )}

      {selectedTaskId && (
        <TaskDetailModal taskId={selectedTaskId} onClose={() => setSelectedTaskId(null)} onTaskChanged={runSearch} />
      )}
    </div>
  );
}