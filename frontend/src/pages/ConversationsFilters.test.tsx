import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import Conversations from './Conversations';
import Modal from '../components/ui/Modal';

vi.mock('../api/conversationsApi', () => ({
  conversationsApi: {
    getAllConversations: vi.fn(),
    getConversationSummary: vi.fn(),
    getConversationDetails: vi.fn(),
    getConversationMessages: vi.fn(),
    updateConversationStatus: vi.fn(),
  },
}));

import { conversationsApi } from '../api/conversationsApi';

const summary = {
  totalConversations: 1,
  activeConversations: 1,
  conversationsToday: 0,
  leadsCaptured: 0,
  averageMessagesPerConversation: 2,
};

const qaConversation = {
  id: 'c1',
  businessId: 'b1',
  businessName: 'Acme',
  customerName: 'Final Retest QA',
  customerEmail: 'qa@example.com',
  channel: 'WEB_WIDGET',
  status: 'ACTIVE',
  messageCount: 2,
  leadCount: 0,
  createdAt: '2026-08-01T00:00:00',
  updatedAt: '2026-08-01T00:00:00',
};

function renderConversations(path = '/conversations') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/conversations" element={<Conversations />} />
      </Routes>
    </MemoryRouter>
  );
}

const qaDetail = {
  ...qaConversation,
  relatedLeads: [],
  voiceCallCount: 0,
};

describe('Conversations search, filters, retry, and accessibility', () => {
  beforeEach(() => {
    vi.mocked(conversationsApi.getConversationSummary).mockResolvedValue(summary as never);
    vi.mocked(conversationsApi.getAllConversations).mockReset();
    vi.mocked(conversationsApi.getConversationDetails).mockResolvedValue(qaDetail as never);
    vi.mocked(conversationsApi.getConversationMessages).mockResolvedValue({
      items: [],
      pagination: { page: 0, size: 100, totalElements: 0, totalPages: 0 },
    } as never);
    vi.mocked(conversationsApi.updateConversationStatus).mockReset();
  });

  it('associates labels with search, status, and icon-only actions', async () => {
    vi.mocked(conversationsApi.getAllConversations).mockResolvedValue({
      items: [],
      pagination: { page: 0, size: 20, totalElements: 0, totalPages: 0 },
    });
    renderConversations();
    const search = await screen.findByPlaceholderText('Search conversations...');
    expect(search).toHaveAccessibleName('Search conversations');
    expect(screen.getByLabelText('Status')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Search conversations' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Refresh conversations' })).toBeInTheDocument();
  });

  it('sends a valid customer search query', async () => {
    const user = userEvent.setup();
    vi.mocked(conversationsApi.getAllConversations).mockResolvedValue({
      items: [qaConversation],
      pagination: { page: 0, size: 20, totalElements: 1, totalPages: 1 },
    } as never);
    renderConversations();
    const search = await screen.findByPlaceholderText('Search conversations...');
    await user.type(search, 'Final Retest QA');
    await user.click(screen.getByRole('button', { name: 'Search conversations' }));
    await waitFor(() => {
      expect(conversationsApi.getAllConversations).toHaveBeenCalledWith(
        expect.objectContaining({ search: 'Final Retest QA' })
      );
    });
    expect(await screen.findByText('Final Retest QA')).toBeInTheDocument();
  });

  it.each(['ACTIVE', 'PAUSED', 'CLOSED', 'ARCHIVED'] as const)(
    'sends status=%s and does not error on empty results',
    async (status) => {
      const user = userEvent.setup();
      vi.mocked(conversationsApi.getAllConversations).mockResolvedValue({
        items: [],
        pagination: { page: 0, size: 20, totalElements: 0, totalPages: 0 },
      });
      renderConversations();
      await screen.findByLabelText('Status');
      await user.selectOptions(screen.getByLabelText('Status'), status);
      await waitFor(() => {
        expect(conversationsApi.getAllConversations).toHaveBeenCalledWith(
          expect.objectContaining({ status })
        );
      });
      expect(await screen.findByText('No conversations match these filters')).toBeInTheDocument();
    }
  );

  it('retries a failed request and recovers when the service returns data', async () => {
    const user = userEvent.setup();
    vi.mocked(conversationsApi.getAllConversations)
      .mockRejectedValueOnce(new Error('Failed to load conversations'))
      .mockResolvedValue({
        items: [qaConversation],
        pagination: { page: 0, size: 20, totalElements: 1, totalPages: 1 },
      } as never);
    renderConversations();
    expect(await screen.findByRole('button', { name: 'Retry' })).toBeInTheDocument();
    expect(screen.queryByText('No conversations yet')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Retry' }));
    expect(await screen.findByText('Final Retest QA')).toBeInTheDocument();
  });

  it('does not send an invalid status from the URL and recovers after clearing filters', async () => {
    const user = userEvent.setup();
    vi.mocked(conversationsApi.getAllConversations).mockResolvedValue({
      items: [qaConversation],
      pagination: { page: 0, size: 20, totalElements: 1, totalPages: 1 },
    } as never);
    renderConversations('/conversations?status=Closed');
    await waitFor(() => {
      expect(conversationsApi.getAllConversations).toHaveBeenCalledWith(
        expect.objectContaining({ status: undefined })
      );
    });
    await user.click(screen.getByRole('button', { name: /Clear/ }));
    await waitFor(() => {
      expect(conversationsApi.getAllConversations).toHaveBeenCalledWith(
        expect.objectContaining({ status: undefined, search: undefined })
      );
    });
  });

  it('resets results when Search is pressed with empty or whitespace input', async () => {
    const user = userEvent.setup();
    const other = { ...qaConversation, id: 'c2', customerName: 'Other Customer' };
    vi.mocked(conversationsApi.getAllConversations).mockImplementation(async (filters) => {
      const items = filters.search ? [qaConversation] : [qaConversation, other];
      return {
        items,
        pagination: { page: 0, size: 20, totalElements: items.length, totalPages: 1 },
      } as never;
    });
    renderConversations('/conversations?search=Final%20Retest%20QA');
    expect(await screen.findByText('Final Retest QA')).toBeInTheDocument();
    expect(screen.queryByText('Other Customer')).not.toBeInTheDocument();

    const search = screen.getByPlaceholderText('Search conversations...');
    await user.clear(search);
    expect(search).toHaveValue('');
    expect(screen.getByRole('button', { name: /Clear/ })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Search conversations' }));
    expect(await screen.findByText('Other Customer')).toBeInTheDocument();
    const calls = vi.mocked(conversationsApi.getAllConversations).mock.calls;
    expect(calls[calls.length - 1][0]).toEqual(expect.objectContaining({ search: undefined }));
    expect(screen.queryByRole('button', { name: /Clear/ })).not.toBeInTheDocument();

    await user.type(search, 'Final Retest QA');
    await user.click(screen.getByRole('button', { name: 'Search conversations' }));
    await waitFor(() => {
      expect(screen.queryByText('Other Customer')).not.toBeInTheDocument();
    });
    await user.clear(search);
    await user.type(search, '   ');
    await user.keyboard('{Enter}');
    expect(await screen.findByText('Other Customer')).toBeInTheDocument();
  });

  it('sends special characters and rejects more than 200 characters in the input', async () => {
    const user = userEvent.setup();
    vi.mocked(conversationsApi.getAllConversations).mockResolvedValue({
      items: [qaConversation],
      pagination: { page: 0, size: 20, totalElements: 1, totalPages: 1 },
    } as never);
    renderConversations();
    const search = await screen.findByPlaceholderText('Search conversations...');
    await user.type(search, 'Acme & Co <test>');
    await user.click(screen.getByRole('button', { name: 'Search conversations' }));
    await waitFor(() => {
      expect(conversationsApi.getAllConversations).toHaveBeenCalledWith(
        expect.objectContaining({ search: 'Acme & Co <test>' })
      );
    });
    expect(search).toHaveAttribute('maxLength', '200');
    await user.clear(search);
    await user.type(search, 'x'.repeat(210));
    expect((search as HTMLInputElement).value.length).toBeLessThanOrEqual(200);
  });

  it('restores results when Clear is used after a search', async () => {
    const user = userEvent.setup();
    const other = { ...qaConversation, id: 'c2', customerName: 'Other Customer' };
    vi.mocked(conversationsApi.getAllConversations).mockImplementation(async (filters) => {
      const items = filters.search ? [qaConversation] : [qaConversation, other];
      return {
        items,
        pagination: { page: 0, size: 20, totalElements: items.length, totalPages: 1 },
      } as never;
    });
    renderConversations('/conversations?search=Final');
    await screen.findByDisplayValue('Final');
    expect(screen.queryByText('Other Customer')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /Clear/ }));
    expect(await screen.findByText('Other Customer')).toBeInTheDocument();
  });

  it('loads additional conversations with Load more', async () => {
    const user = userEvent.setup();
    const firstPage = Array.from({ length: 20 }, (_, index) => ({
      ...qaConversation,
      id: `c${index}`,
      customerName: `Customer ${index}`,
    }));
    vi.mocked(conversationsApi.getAllConversations)
      .mockResolvedValueOnce({
        items: firstPage,
        pagination: { page: 0, size: 20, totalElements: 26, totalPages: 2 },
      } as never)
      .mockResolvedValueOnce({
        items: [{ ...qaConversation, id: 'c20', customerName: 'Customer 20' }],
        pagination: { page: 1, size: 20, totalElements: 26, totalPages: 2 },
      } as never);
    renderConversations();
    expect(await screen.findByText('Showing 20 of 26')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Load more' }));
    await waitFor(() => {
      expect(conversationsApi.getAllConversations).toHaveBeenCalledWith(
        expect.objectContaining({ page: 1, size: 20 })
      );
    });
    expect(await screen.findByText('Showing 21 of 26')).toBeInTheDocument();
  });

  it('dismisses the detail panel without changing conversation status', async () => {
    const user = userEvent.setup();
    vi.mocked(conversationsApi.getAllConversations).mockResolvedValue({
      items: [qaConversation],
      pagination: { page: 0, size: 20, totalElements: 1, totalPages: 1 },
    } as never);
    renderConversations();
    await user.click(await screen.findByText('Final Retest QA'));
    expect(await screen.findByRole('dialog', { name: 'Final Retest QA' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Dismiss' }));
    expect(screen.queryByRole('dialog', { name: 'Final Retest QA' })).not.toBeInTheDocument();
    expect(conversationsApi.updateConversationStatus).not.toHaveBeenCalled();
  });

  it('marks a conversation closed with the status action', async () => {
    const user = userEvent.setup();
    vi.mocked(conversationsApi.getAllConversations).mockResolvedValue({
      items: [qaConversation],
      pagination: { page: 0, size: 20, totalElements: 1, totalPages: 1 },
    } as never);
    vi.mocked(conversationsApi.updateConversationStatus).mockResolvedValue({
      ...qaDetail,
      status: 'CLOSED',
    } as never);
    renderConversations();
    await user.click(await screen.findByText('Final Retest QA'));
    await user.click(await screen.findByRole('button', { name: 'Mark closed' }));
    expect(conversationsApi.updateConversationStatus).toHaveBeenCalledWith('c1', { status: 'CLOSED' });
  });
});

describe('modal keyboard behavior', () => {
  it('has a dialog name, traps focus, and closes on Escape', async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();
    render(
      <Modal title="Run Details" onClose={onClose}>
        <button type="button">Inside</button>
      </Modal>
    );
    expect(screen.getByRole('dialog', { name: 'Run Details' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Close Run Details' })).toHaveFocus();
    await user.keyboard('{Escape}');
    expect(onClose).toHaveBeenCalled();
  });
});
