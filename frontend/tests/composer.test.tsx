import { fireEvent, render, screen } from '@testing-library/react';
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
    yield { type: 'token', text: 'Working on it.' } as const;
    await gate;
    yield { type: 'done' } as const;
  })();
}

describe('composer (US3 and US4, T024)', () => {
  beforeEach(() => {
    streamChat.mockReset();
  });

  it('keeps the textarea editable and disables only Send while a reply streams (FR-019)', async () => {
    streamChat.mockImplementation(() => controlledReply());
    render(<App />);

    await userEvent.type(screen.getByLabelText('Your question'), 'First question');
    await userEvent.click(screen.getByRole('button', { name: 'Send' }));
    await screen.findByText('Working on it.');

    const textarea = screen.getByLabelText('Your question');
    expect(textarea).not.toBeDisabled();
    expect(screen.getByRole('button', { name: 'Send' })).toBeDisabled();

    await userEvent.type(textarea, 'draft');
    expect(textarea).toHaveValue('draft');
    release();
  });

  it('shows the counter from 80 percent of the limit (FR-020)', () => {
    streamChat.mockImplementation(() => controlledReply());
    render(<App />);
    const textarea = screen.getByLabelText('Your question');

    fireEvent.change(textarea, { target: { value: 'a'.repeat(1599) } });
    expect(screen.queryByText(/ of 2,000$/)).toBeNull();

    fireEvent.change(textarea, { target: { value: 'a'.repeat(1600) } });
    expect(screen.getByText('1,600 of 2,000')).toBeInTheDocument();
  });

  it('uses the coral treatment above the limit (FR-020)', () => {
    render(<App />);
    fireEvent.change(screen.getByLabelText('Your question'), { target: { value: 'a'.repeat(2001) } });

    expect(screen.getByText('2,001 of 2,000')).toHaveClass('counter-over');
  });

  it('refuses a blank message with a plain note (FR-005)', async () => {
    render(<App />);
    await userEvent.click(screen.getByRole('button', { name: 'Send' }));

    expect(screen.getByRole('alert')).toHaveTextContent('A message is required.');
    expect(streamChat).not.toHaveBeenCalled();
  });

  it('refuses a message over the limit with the existing note (FR-005)', async () => {
    render(<App />);
    fireEvent.change(screen.getByLabelText('Your question'), { target: { value: 'a'.repeat(2001) } });
    await userEvent.click(screen.getByRole('button', { name: 'Send' }));

    expect(screen.getByRole('alert')).toHaveTextContent('Messages can be at most 2000 characters.');
    expect(streamChat).not.toHaveBeenCalled();
  });
});
