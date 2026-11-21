import { render, screen } from '@testing-library/react';
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
  yield { type: 'token', text: 'Answer.' } as const;
  yield { type: 'done' } as const;
}

describe('header and start over (US6, T043)', () => {
  beforeEach(() => {
    streamChat.mockReset();
    streamChat.mockImplementation(() => quickReply());
  });

  it('shows the title, hides the decorative mark from assistive technology, and disables New conversation when empty', () => {
    const { container } = render(<App />);

    expect(screen.getByRole('heading', { level: 1, name: 'Caramel AI Tech Support' })).toBeInTheDocument();
    expect(container.querySelector('.mark')).toHaveAttribute('aria-hidden', 'true');
    expect(screen.getByRole('button', { name: 'New conversation' })).toBeDisabled();
  });

  it('enables New conversation after a message', async () => {
    render(<App />);
    await userEvent.type(screen.getByLabelText('Your question'), 'Hello');
    await userEvent.click(screen.getByRole('button', { name: 'Send' }));
    await screen.findByText('Answer.');

    expect(screen.getByRole('button', { name: 'New conversation' })).toBeEnabled();
  });

  it('clears the messages, shows the starters, and focuses the composer', async () => {
    render(<App />);
    await userEvent.type(screen.getByLabelText('Your question'), 'Hello');
    await userEvent.click(screen.getByRole('button', { name: 'Send' }));
    await screen.findByText('Answer.');

    await userEvent.click(screen.getByRole('button', { name: 'New conversation' }));

    expect(screen.queryByText('Answer.')).toBeNull();
    expect(screen.getByRole('heading', { name: 'Suggested questions' })).toBeInTheDocument();
    expect(screen.getByLabelText('Your question')).toHaveFocus();
  });
});
