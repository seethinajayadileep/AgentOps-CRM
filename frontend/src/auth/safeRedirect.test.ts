import { describe, expect, it } from 'vitest';
import { safeInternalPath } from './safeRedirect';

describe('safeInternalPath', () => {
  it('keeps an internal CRM path', () => {
    expect(safeInternalPath('/leads')).toBe('/leads');
    expect(safeInternalPath('/businesses?q=acme')).toBe('/businesses?q=acme');
  });

  it('rejects external or protocol-relative values', () => {
    expect(safeInternalPath('https://evil.example/phish')).toBe('/dashboard');
    expect(safeInternalPath('//evil.example')).toBe('/dashboard');
    expect(safeInternalPath('dashboard')).toBe('/dashboard');
  });

  it('does not bounce back to auth pages', () => {
    expect(safeInternalPath('/login')).toBe('/dashboard');
    expect(safeInternalPath('/signup?x=1')).toBe('/dashboard');
  });
});
