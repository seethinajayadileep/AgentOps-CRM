import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import VoiceCalls from './VoiceCalls';

vi.mock('../api/voiceCallsApi', () => ({
  voiceCallsApi: {
    getAllCalls: vi.fn(),
    getRecording: vi.fn(),
  },
}));

import { voiceCallsApi } from '../api/voiceCallsApi';

const completedCall = {
  id: 'v1',
  leadId: 'l1',
  leadName: 'Pat Lee',
  phoneNumber: '+15551212',
  status: 'COMPLETED',
  provider: 'vapi',
  outcome: 'ANSWERED',
  createdAt: '2026-08-01T00:00:00',
};

describe('voice-call filtered empty state', () => {
  beforeEach(() => {
    vi.mocked(voiceCallsApi.getAllCalls).mockReset();
    vi.mocked(voiceCallsApi.getRecording).mockReset();
    Object.defineProperty(URL, 'createObjectURL', {
      configurable: true,
      writable: true,
      value: vi.fn(() => 'blob:http://localhost/crm-recording'),
    });
    Object.defineProperty(URL, 'revokeObjectURL', {
      configurable: true,
      writable: true,
      value: vi.fn(),
    });
  });

  it('reserves the account-empty copy when there are no calls', async () => {
    vi.mocked(voiceCallsApi.getAllCalls).mockResolvedValue({
      items: [],
      total: 0,
      page: 0,
      pageSize: 100,
      totalPages: 0,
    });
    render(
      <MemoryRouter>
        <VoiceCalls />
      </MemoryRouter>
    );
    expect(await screen.findByText('Voice calls will appear here…')).toBeInTheDocument();
    expect(screen.queryByText('No voice calls match the selected filters.')).not.toBeInTheDocument();
  });

  it('shows filter-specific copy and Clear Filters when filters exclude every call', async () => {
    const user = userEvent.setup();
    vi.mocked(voiceCallsApi.getAllCalls).mockResolvedValue({
      items: [completedCall],
      total: 1,
      page: 0,
      pageSize: 100,
      totalPages: 1,
    } as never);
    render(
      <MemoryRouter>
        <VoiceCalls />
      </MemoryRouter>
    );
    expect(await screen.findByText('Pat Lee')).toBeInTheDocument();
    await user.selectOptions(screen.getByLabelText('Status'), 'FAILED');
    expect(screen.getByText('No voice calls match the selected filters.')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Clear Filters' }));
    expect(screen.getByText('Pat Lee')).toBeInTheDocument();
  });

  it('opens call details with a single heading and one labelled close control', async () => {
    const user = userEvent.setup();
    vi.mocked(voiceCallsApi.getAllCalls).mockResolvedValue({
      items: [{ ...completedCall, leadName: 'Jaya' }],
      total: 1,
      page: 0,
      pageSize: 100,
      totalPages: 1,
    } as never);
    render(
      <MemoryRouter>
        <VoiceCalls />
      </MemoryRouter>
    );
    await user.click(await screen.findByRole('button', { name: 'View' }));
    expect(screen.getByRole('dialog', { name: 'Call with Jaya' })).toBeInTheDocument();
    expect(screen.getAllByRole('heading', { name: 'Call with Jaya' })).toHaveLength(1);
    expect(screen.getAllByRole('button', { name: /close/i })).toHaveLength(1);
    expect(screen.getByRole('button', { name: 'Close Call with Jaya' })).toHaveFocus();
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog', { name: 'Call with Jaya' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'View' })).toHaveFocus();
  });

  it('plays recordings from a same-origin blob instead of the provider URL', async () => {
    const user = userEvent.setup();
    vi.mocked(voiceCallsApi.getAllCalls).mockResolvedValue({
      items: [
        {
          ...completedCall,
          recordingUrl: 'https://storage.vapi.ai/secret-recording.mp3',
        },
      ],
      total: 1,
      page: 0,
      pageSize: 100,
      totalPages: 1,
    } as never);
    vi.mocked(voiceCallsApi.getRecording).mockResolvedValue(
      new Blob([new Uint8Array([1, 2, 3])], { type: 'audio/mpeg' })
    );
    render(
      <MemoryRouter>
        <VoiceCalls />
      </MemoryRouter>
    );
    await user.click(await screen.findByRole('button', { name: 'View' }));
    const audio = await waitFor(() => {
      const el = document.querySelector('[data-testid="voice-recording-player"]');
      expect(el?.getAttribute('src')).toMatch(/^blob:/);
      return el;
    });
    expect(voiceCallsApi.getRecording).toHaveBeenCalledWith('v1');
    expect(audio?.getAttribute('src')).not.toContain('vapi.ai');
    expect(audio?.getAttribute('src')).not.toMatch(/\/voice-calls\/v1\/recording$/);
  });

  it('shows an error when the recording proxy fails', async () => {
    const user = userEvent.setup();
    vi.mocked(voiceCallsApi.getAllCalls).mockResolvedValue({
      items: [
        {
          ...completedCall,
          recordingUrl: 'https://storage.vapi.ai/secret-recording.mp3',
        },
      ],
      total: 1,
      page: 0,
      pageSize: 100,
      totalPages: 1,
    } as never);
    vi.mocked(voiceCallsApi.getRecording).mockRejectedValue(new Error('502'));
    render(
      <MemoryRouter>
        <VoiceCalls />
      </MemoryRouter>
    );
    await user.click(await screen.findByRole('button', { name: 'View' }));
    expect(
      await screen.findByText(
        'Recording could not be played. The file may still be processing or is no longer available.'
      )
    ).toBeInTheDocument();
    expect(document.querySelector('audio')).toBeNull();
  });
});
