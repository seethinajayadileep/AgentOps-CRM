import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import Landing from './Landing';

describe('Landing page', () => {
  it('renders the product headline and primary actions', () => {
    render(
      <MemoryRouter>
        <Landing />
      </MemoryRouter>
    );
    expect(
      screen.getByRole('heading', {
        name: 'A CRM that doesn’t just track work—it moves it forward.',
      })
    ).toBeInTheDocument();
    expect(screen.getAllByRole('link', { name: 'Get Started' })[0]).toHaveAttribute('href', '/signup');
    expect(screen.getAllByRole('link', { name: 'Login' })[0]).toHaveAttribute('href', '/login');
    expect(screen.getByRole('link', { name: 'Explore the product' })).toHaveAttribute('href', '/login');
  });
});
