import type { ReactNode } from 'react';
import { Navigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';
import { safeInternalPath } from '../../auth/safeRedirect';
import SessionLoading from './SessionLoading';

export default function GuestRoute({ children }: { children: ReactNode }) {
  const { status } = useAuth();
  const [params] = useSearchParams();

  if (status === 'loading') {
    return <SessionLoading />;
  }

  if (status === 'authenticated') {
    return <Navigate to={safeInternalPath(params.get('redirect'))} replace />;
  }

  return children;
}
