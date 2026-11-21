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

async function* failedBeforeText() {
  yield { type: 'error', code: 'REPLY_TIMEOUT' } as const;
}

async function* cutShort() {
  yield { type: 'token', text: 'You are seeing a 429 because ' } as const;
  yield { type: 'error', code: 'UPSTREAM_UNAVAILABLE' } as const;
}

async function ask(question: string) {
  await userEvent.type(screen.getByLabelText('Your question'), question);
  await userEvent.click(screen.getByRole('button', { name: 'Send' }));
}

describe('failure notice (US4, T034)', () => {
  beforeEach(() => {
    streamChat.mockReset();
  });

  it('shows the friendly text and a Try again pill for a failed reply, with no code visible', async () => {
    streamChat.mockReturnValueOnce(failedBeforeText());
    render(<App />);
    await ask('Slow?');

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('This is taking longer than expected.');
    expect(screen.getByRole('button', { name: 'Try again' })).toHaveClass('pill-outline-coral');
    expect(document.body.textContent).not.toContain('REPLY_TIMEOUT');
  });

  it('labels a cut-short reply, keeps its partial text, and offers Try again (FR-004)', async () => {
    streamChat.mockReturnValueOnce(cutShort());
    render(<App />);
    await ask('Why 429?');

    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Reply was cut short.'));
    expect(screen.getByText('You are seeing a 429 because')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument();
    expect(document.body.textContent).not.toContain('UPSTREAM_UNAVAILABLE');
  });
});
