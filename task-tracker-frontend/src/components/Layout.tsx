import { useEffect, useState } from 'react';
import { NavLink, Routes, Route } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { apiRequest } from '../api/client';
import { DashboardPage } from '../pages/DashboardPage';
import { ProjectsPage } from '../pages/ProjectsPage';
import { ProjectDetailPage } from '../pages/ProjectDetailPage';
import { MyTasksPage } from '../pages/MyTasksPage';
import { AlertsPage } from '../pages/AlertsPage';

export function Layout() {
  const { user, logout } = useAuth();
  const [alertCount, setAlertCount] = useState(0);

  useEffect(() => {
    apiRequest<{ count: number }>('/api/alerts/count')
      .then((data) => setAlertCount(data.count))
      .catch(() => setAlertCount(0));
  }, []);

  const navLinkClass = ({ isActive }: { isActive: boolean }) =>
    `px-3 py-1.5 rounded-md text-sm font-medium transition-colors ${
      isActive ? 'bg-primary/10 text-primary' : 'text-ink-soft hover:text-ink'
    }`;

  return (
    <div className="min-h-screen bg-paper">
      <nav className="bg-white border-b border-line px-6 py-3 flex items-center justify-between">
        <div className="flex items-center gap-6">
          <span className="font-display text-lg font-semibold text-ink">Waypoint</span>
          <div className="flex items-center gap-1">
            <NavLink to="/" end className={navLinkClass}>Dashboard</NavLink>
            <NavLink to="/projects" className={navLinkClass}>Projects</NavLink>
            <NavLink to="/my-tasks" className={navLinkClass}>My tasks</NavLink>
            <NavLink to="/alerts" className={navLinkClass}>
              <span className="inline-flex items-center gap-1.5">
                Alerts
                {alertCount > 0 && (
                  <span className="bg-amber text-white text-xs font-semibold rounded-full min-w-[1.25rem] h-5 flex items-center justify-center px-1">
                    {alertCount}
                  </span>
                )}
              </span>
            </NavLink>
          </div>
        </div>
        <div className="flex items-center gap-4 text-sm">
          <span className="text-ink-soft">
            {user?.email} <span className="text-ink-soft/60">({user?.role.toLowerCase()})</span>
          </span>
          <button onClick={logout} className="text-ink-soft hover:text-danger transition-colors font-medium">
            Log out
          </button>
        </div>
      </nav>
      <main className="p-6">
        <Routes>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/projects" element={<ProjectsPage />} />
          <Route path="/projects/:id" element={<ProjectDetailPage />} />
          <Route path="/my-tasks" element={<MyTasksPage />} />
          <Route path="/alerts" element={<AlertsPage />} />
        </Routes>
      </main>
    </div>
  );
}