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
  yield { type: 'token', text: 'Done.' } as const;
  yield { type: 'done' } as const;
}

describe('focus return to the composer (US3, T025)', () => {
  beforeEach(() => {
    streamChat.mockReset();
    streamChat.mockImplementation(() => quickReply());
  });

  it('moves focus to the textarea when a reply finishes (FR-021)', async () => {
    render(<App />);
    await userEvent.type(screen.getByLabelText('Your question'), 'Question');
    await userEvent.click(screen.getByRole('button', { name: 'Send' }));

    await screen.findByText('Done.');
    await waitFor(() => expect(screen.getByLabelText('Your question')).toHaveFocus());
  });

  it('moves focus to the textarea after New conversation (FR-021)', async () => {
    render(<App />);
    await userEvent.type(screen.getByLabelText('Your question'), 'Question');
    await userEvent.click(screen.getByRole('button', { name: 'Send' }));
    await screen.findByText('Done.');

    await userEvent.click(screen.getByRole('button', { name: 'New conversation' }));
    expect(screen.getByLabelText('Your question')).toHaveFocus();
  });
});
