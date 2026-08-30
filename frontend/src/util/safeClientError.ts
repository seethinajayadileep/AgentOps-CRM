const TECHNICAL = /pkix|certificate|javax\.|java\.|https?:\/\/|stack|caused by|sslhandshake|apify\.com/i;

export function safeClientError(error: unknown, fallback = 'Something went wrong. Please try again.'): string {
  const raw =
    typeof error === 'string'
      ? error
      : error && typeof error === 'object' && 'message' in error
        ? String((error as { message?: unknown }).message || '')
        : '';
  if (!raw || TECHNICAL.test(raw)) {
    return fallback;
  }
  return raw;
}
