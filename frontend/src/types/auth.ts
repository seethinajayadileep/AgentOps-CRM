export interface AuthUser {
  id: string;
  fullName: string;
  email: string;
  externalActionsDisabled: boolean;
  sharedWorkspace: boolean;
}

export interface AuthSession {
  token: string;
  user: AuthUser;
}

export type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated';
