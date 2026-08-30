import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import NotFound from '../pages/NotFound';
import { APP_VERSION } from '../config/version';
import packageJson from '../../package.json';

describe('404 routing page', () => {
  it('explains the missing page and offers dashboard and back actions', () => {
    render(
      <MemoryRouter>
        <NotFound />
      </MemoryRouter>
    );
    expect(screen.getByRole('heading', { name: 'Page not found' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Return to Dashboard' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Go back' })).toBeInTheDocument();
  });
});

describe('version consistency', () => {
  it('matches package.json', () => {
    expect(APP_VERSION).toBe(packageJson.version);
    expect(APP_VERSION).toBe('0.2.0');
  });
});
