import { describe, expect, it } from 'vitest';
import { safeClientError } from './safeClientError';

describe('safeClientError', () => {
  it('keeps a user-facing message', () => {
    expect(safeClientError('Search name is required')).toBe('Search name is required');
  });

  it('strips provider URLs, Java types, and TLS details', () => {
    expect(
      safeClientError(
        'PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException https://api.apify.com/v2',
        'Lead discovery failed'
      )
    ).toBe('Lead discovery failed');
  });
});
