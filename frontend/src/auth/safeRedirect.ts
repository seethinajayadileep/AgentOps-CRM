const AUTH_PATHS = ['/login', '/signup', '/forgot-password'];

export function safeInternalPath(value: string | null | undefined): string {
  if (!value) {
    return '/dashboard';
  }
  const trimmed = value.trim();
  if (!trimmed.startsWith('/') || trimmed.startsWith('//') || trimmed.includes('://')) {
    return '/dashboard';
  }
  if (AUTH_PATHS.some((path) => trimmed === path || trimmed.startsWith(`${path}?`))) {
    return '/dashboard';
  }
  return trimmed;
}
