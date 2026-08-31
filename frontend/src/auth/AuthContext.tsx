import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import { authApi, TOKEN_STORAGE_KEY } from '../api/authApi';
import type { AuthSession, AuthStatus, AuthUser } from '../types/auth';

interface AuthContextValue {
  status: AuthStatus;
  user: AuthUser | null;
  login: (email: string, password: string) => Promise<void>;
  signup: (fullName: string, email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function storage(): Storage | null {
  try {
    return typeof localStorage === 'undefined' ? null : localStorage;
  } catch {
    return null;
  }
}

function persistSession(session: AuthSession) {
  storage()?.setItem(TOKEN_STORAGE_KEY, session.token);
}

function clearSession() {
  storage()?.removeItem(TOKEN_STORAGE_KEY);
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>('loading');
  const [user, setUser] = useState<AuthUser | null>(null);

  const restore = useCallback(async () => {
    const token = storage()?.getItem(TOKEN_STORAGE_KEY);
    if (!token) {
      setUser(null);
      setStatus('unauthenticated');
      return;
    }
    try {
      const current = await authApi.me();
      setUser(current);
      setStatus('authenticated');
    } catch {
      clearSession();
      setUser(null);
      setStatus('unauthenticated');
    }
  }, []);

  useEffect(() => {
    void restore();
  }, [restore]);

  useEffect(() => {
    const onUnauthorized = () => {
      clearSession();
      setUser(null);
      setStatus('unauthenticated');
    };
    window.addEventListener('auth:unauthorized', onUnauthorized);
    return () => window.removeEventListener('auth:unauthorized', onUnauthorized);
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const session = await authApi.login(email, password);
    persistSession(session);
    setUser(session.user);
    setStatus('authenticated');
  }, []);

  const signup = useCallback(async (fullName: string, email: string, password: string) => {
    const session = await authApi.signup(fullName, email, password);
    persistSession(session);
    setUser(session.user);
    setStatus('authenticated');
  }, []);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } catch {
      // Local session must still clear if the API is unreachable.
    }
    clearSession();
    setUser(null);
    setStatus('unauthenticated');
  }, []);

  const value = useMemo(
    () => ({ status, user, login, signup, logout }),
    [status, user, login, signup, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}
