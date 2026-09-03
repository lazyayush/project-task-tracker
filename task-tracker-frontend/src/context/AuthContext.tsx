import { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import { apiRequest, setToken } from '../api/client';

interface User {
  email: string;
  role: 'MANAGER' | 'MEMBER';
}

interface AuthContextValue {
  user: User | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, role: 'MANAGER' | 'MEMBER') => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) {
      setLoading(false);
      return;
    }

    apiRequest<{ email: string; authorities: { authority: string }[] }>('/api/me')
      .then((data) => {
        const role = data.authorities.some((a) => a.authority === 'ROLE_MANAGER') ? 'MANAGER' : 'MEMBER';
        setUser({ email: data.email, role });
      })
      .catch(() => setToken(null))
      .finally(() => setLoading(false));
  }, []);

  async function login(email: string, password: string) {
    const data = await apiRequest<{ token: string; email: string; role: 'MANAGER' | 'MEMBER' }>('/api/auth/login', {
      method: 'POST',
      body: { email, password },
    });
    setToken(data.token);
    setUser({ email: data.email, role: data.role });
  }

  async function register(email: string, password: string, role: 'MANAGER' | 'MEMBER') {
    const data = await apiRequest<{ token: string; email: string; role: 'MANAGER' | 'MEMBER' }>('/api/auth/register', {
      method: 'POST',
      body: { email, password, role },
    });
    setToken(data.token);
    setUser({ email: data.email, role: data.role });
  }

  function logout() {
    setToken(null);
    setUser(null);
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within an AuthProvider');
  return context;
}