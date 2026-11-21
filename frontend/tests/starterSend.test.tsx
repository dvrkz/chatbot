import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { App } from '../src/App';
import * as chatApi from '../src/services/chatApi';

vi.mock('../src/services/chatApi', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../src/services/chatApi')>();
  return { ...actual, streamChat: vi.fn(), clearSession: vi.fn(() => Promise.resolve()) };
});

const streamChat = vi.mocked(chatApi.streamChat);

async function* quickReply() {
  yield { type: 'token', text: 'Quick answer.' } as const;
  yield { type: 'done' } as const;
}

describe('starter questions (US1, T013)', () => {
  beforeEach(() => {
    streamChat.mockReset();
    streamChat.mockImplementation(() => quickReply());
  });

  it('sends the chosen starter exactly as if typed, then hides the starters', async () => {
    render(<App />);
    await userEvent.click(screen.getByRole('button', { name: 'How do I stream a response?' }));

    expect(streamChat).toHaveBeenCalledWith(expect.any(String), 'How do I stream a response?', expect.anything());
    await screen.findByText('Quick answer.');
    expect(screen.queryByRole('heading', { name: 'Suggested questions' })).toBeNull();
  });

  it('brings the starters back after New conversation', async () => {
    render(<App />);
    await userEvent.click(screen.getByRole('button', { name: 'How do I choose a model?' }));
    await screen.findByText('Quick answer.');

    await userEvent.click(screen.getByRole('button', { name: 'New conversation' }));
    expect(screen.getByRole('heading', { name: 'Suggested questions' })).toBeInTheDocument();
  });

  it('sends a typed question the same way', async () => {
    render(<App />);
    await userEvent.type(screen.getByLabelText('Your question'), 'Typed question');
    await userEvent.click(screen.getByRole('button', { name: 'Send' }));

    await waitFor(() => expect(streamChat).toHaveBeenCalledWith(expect.any(String), 'Typed question', expect.anything()));
  });
});
