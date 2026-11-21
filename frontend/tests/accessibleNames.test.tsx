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

let release: () => void = () => undefined;
function controlledReply() {
  const gate = new Promise<void>((resolve) => {
    release = resolve;
  });
  return (async function* () {
    yield { type: 'token', text: 'Checking.' } as const;
    await gate;
    yield { type: 'done' } as const;
  })();
}

describe('accessible names and roles (US5, T037; prd-ui-2 section 9)', () => {
  beforeEach(() => {
    streamChat.mockReset();
  });

  it('keeps the composer label, the Send and New conversation names, and the disabled header action', () => {
    render(<App />);

    expect(screen.getByLabelText('Your question')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Send' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'New conversation' })).toBeDisabled();
  });

  it('announces the typing status, and marks the list busy only while a reply streams', async () => {
    streamChat.mockImplementation(() => controlledReply());
    render(<App />);

    await userEvent.type(screen.getByLabelText('Your question'), 'Question');
    await userEvent.click(screen.getByRole('button', { name: 'Send' }));
    await screen.findByText('Checking.');

    expect(screen.getByRole('status', { name: 'Assistant is typing' })).toBeInTheDocument();
    const list = screen.getByRole('list');
    expect(list).toHaveAttribute('aria-live', 'polite');
    expect(list).toHaveAttribute('aria-busy', 'true');

    release();
    await waitFor(() => expect(list).toHaveAttribute('aria-busy', 'false'));
  });

  it('names the Try again action and announces failures as alerts', async () => {
    streamChat.mockImplementation(() =>
      (async function* () {
        yield { type: 'error', code: 'RATE_LIMITED' } as const;
      })(),
    );
    render(<App />);

    await userEvent.type(screen.getByLabelText('Your question'), 'Too fast');
    await userEvent.click(screen.getByRole('button', { name: 'Send' }));

    expect(await screen.findByRole('alert')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument();
  });
});
