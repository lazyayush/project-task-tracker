import { useEffect, useState, type FormEvent } from 'react';
import { fetchTaskHistory, postComment, type HistoryEntry } from '../api/history';
import { ApiError } from '../api/client';

const FIELD_LABELS: Record<string, string> = {
  title: 'Title', description: 'Description', priority: 'Priority',
  dueDate: 'Due date', status: 'Status', assignee: 'Assignee',
};

function formatHistoryValue(fieldName: string | null, value: string | null): string {
  if (value === null) return '—';

  if (fieldName === 'dueDate') {
    const date = new Date(value);
    if (isNaN(date.getTime())) return value;
    return date.toLocaleString(undefined, {
      dateStyle: 'medium',
      timeStyle: 'short',
    });
  }

  return value;
}

function describeEntry(entry: HistoryEntry): string {
  const field = entry.fieldName ? (FIELD_LABELS[entry.fieldName] ?? entry.fieldName) : '';
  const oldVal = formatHistoryValue(entry.fieldName, entry.oldValue);
  const newVal = formatHistoryValue(entry.fieldName, entry.newValue);

  switch (entry.eventType) {
    case 'CREATED':
      return 'created this task';
    case 'FIELD_CHANGED':
      return `changed ${field} from "${oldVal}" to "${newVal}"`;
    case 'ASSIGNED':
      return `assigned ${entry.newValue}`;
    case 'UNASSIGNED':
      return `unassigned ${entry.oldValue}`;
    case 'COMMENT':
      return 'commented';
    default:
      return '';
  }
}

export function TaskHistoryPanel({ taskId }: { taskId: number }) {
  const [entries, setEntries] = useState<HistoryEntry[] | null>(null);
  const [commentText, setCommentText] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function reload() {
    fetchTaskHistory(taskId).then((data) => setEntries([...data].reverse())).catch(() => setEntries([]));
  }

  useEffect(reload, [taskId]);

  async function handleComment(e: FormEvent) {
    e.preventDefault();
    if (!commentText.trim()) return;
    setError(null);
    setSubmitting(true);
    try {
      await postComment(taskId, commentText);
      setCommentText('');
      reload();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not post comment');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="bg-white border border-line rounded-lg p-4">
      <h3 className="text-sm font-semibold text-ink mb-3">History &amp; comments</h3>

      <form onSubmit={handleComment} className="flex flex-col gap-2 mb-4">
        <textarea
          value={commentText}
          onChange={(e) => setCommentText(e.target.value)}
          placeholder="Add a comment…"
          rows={2}
          className="border border-line rounded-lg px-3 py-2 text-sm text-ink focus:outline-none focus:ring-2 focus:ring-primary/30 focus:border-primary resize-none"
        />
        {error && <p className="text-danger text-xs">{error}</p>}
        <button
          type="submit"
          disabled={submitting || !commentText.trim()}
          className="bg-primary hover:bg-primary-dark text-white text-xs font-medium rounded-lg px-3 py-1.5 w-fit transition-colors disabled:opacity-50"
        >
          {submitting ? 'Posting…' : 'Comment'}
        </button>
      </form>

      {!entries ? (
        <p className="text-sm text-ink-soft">Loading…</p>
      ) : entries.length === 0 ? (
        <p className="text-sm text-ink-soft">No activity yet.</p>
      ) : (
        <div className="flex flex-col gap-3 max-h-80 overflow-y-auto">
          {entries.map((entry) => (
            <div key={entry.id} className="text-sm border-l-2 border-line pl-3">
              <p className="text-ink-soft">
                <span className="font-medium text-ink">{entry.actorEmail}</span> {describeEntry(entry)}
              </p>
              {entry.eventType === 'COMMENT' && (
                <p className="text-ink mt-1 bg-ink/[0.03] rounded px-2 py-1.5">{entry.commentText}</p>
              )}
              <p className="text-xs text-ink-soft/60 mt-0.5">
                {new Date(entry.createdAt).toLocaleString()}
              </p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
