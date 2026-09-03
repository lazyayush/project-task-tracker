import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ApiError } from '../api/client';

export function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState<'MANAGER' | 'MEMBER'>('MEMBER');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await register(email, password, role);
      navigate('/');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Registration failed');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="min-h-screen flex">
      <div className="hidden md:flex md:w-2/5 bg-primary flex-col justify-between p-12">
        <span className="font-display text-2xl font-semibold text-white">Waypoint</span>
        <div>
          <p className="font-display text-3xl font-medium text-white leading-snug">
            Set your course.
          </p>
          <p className="text-white/60 text-sm mt-4 max-w-xs">
            Create an account to start tracking projects and tasks.
          </p>
        </div>
        <span className="text-white/40 text-xs">Internal use only</span>
      </div>

      <div className="flex-1 flex items-center justify-center p-8 bg-paper">
        <form onSubmit={handleSubmit} className="w-full max-w-sm flex flex-col gap-5">
          <div>
            <h1 className="font-display text-2xl font-semibold text-ink">Create account</h1>
            <p className="text-ink-soft text-sm mt-1">Choose your role to get started.</p>
          </div>

          {error && (
            <p className="text-danger text-sm bg-danger/5 border border-danger/20 rounded px-3 py-2">{error}</p>
          )}

          <div className="flex flex-col gap-1.5">
            <label htmlFor="email" className="text-sm font-medium text-ink-soft">Email</label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="border border-line rounded-lg px-3 py-2.5 text-ink placeholder:text-ink-soft/50 focus:outline-none focus:ring-2 focus:ring-primary/30 focus:border-primary"
              required
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <label htmlFor="password" className="text-sm font-medium text-ink-soft">Password</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              minLength={8}
              className="border border-line rounded-lg px-3 py-2.5 text-ink focus:outline-none focus:ring-2 focus:ring-primary/30 focus:border-primary"
              required
            />
            <span className="text-xs text-ink-soft/70">At least 8 characters</span>
          </div>

          <div className="flex flex-col gap-1.5">
            <label htmlFor="role" className="text-sm font-medium text-ink-soft">Role</label>
            <select
              id="role"
              value={role}
              onChange={(e) => setRole(e.target.value as 'MANAGER' | 'MEMBER')}
              className="border border-line rounded-lg px-3 py-2.5 text-ink focus:outline-none focus:ring-2 focus:ring-primary/30 focus:border-primary bg-white"
            >
              <option value="MEMBER">Member</option>
              <option value="MANAGER">Manager</option>
            </select>
          </div>

          <button
            type="submit"
            disabled={submitting}
            className="bg-primary hover:bg-primary-dark text-white font-medium rounded-lg px-4 py-2.5 mt-2 transition-colors disabled:opacity-50"
          >
            {submitting ? 'Creating account…' : 'Create account'}
          </button>

          <p className="text-sm text-ink-soft text-center">
            Already have an account? <Link to="/login" className="text-primary font-medium hover:underline">Log in</Link>
          </p>
        </form>
      </div>
    </div>
  );
}