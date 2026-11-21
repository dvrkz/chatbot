import { act, render, renderHook, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { App } from '../src/App';
import { useConversation } from '../src/hooks/useConversation';
import * as chatApi from '../src/services/chatApi';

vi.mock('../src/services/chatApi', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../src/services/chatApi')>();
  return { ...actual, streamChat: vi.fn(), clearSession: vi.fn(() => Promise.resolve()) };
});

const streamChat = vi.mocked(chatApi.streamChat);

async function* partialThenError() {
  yield { type: 'token', text: 'You are seeing a 429 because ' } as const;
  yield { type: 'error', code: 'UPSTREAM_UNAVAILABLE' } as const;
}

async function* errorOnly() {
  yield { type: 'error', code: 'REPLY_TIMEOUT' } as const;
}

async function* completeReply() {
  yield { type: 'token', text: 'Fixed.' } as const;
  yield { type: 'done' } as const;
}

describe('failure handling', () => {
  beforeEach(() => {
    streamChat.mockReset();
  });

  it('keeps partial text, marks it incomplete, and offers retry', async () => {
    streamChat.mockReturnValueOnce(partialThenError());
    const { result } = renderHook(() => useConversation());

    await act(async () => {
      await result.current.send('Why 429?');
    });

    const reply = result.current.messages[1];
    expect(reply.status).toBe('incomplete');
    expect(reply.text).toBe('You are seeing a 429 because ');
  });

  it('marks a reply with no text as failed', async () => {
    streamChat.mockReturnValueOnce(errorOnly());
    const { result } = renderHook(() => useConversation());

    await act(async () => {
      await result.current.send('Slow?');
    });

    expect(result.current.messages[1].status).toBe('failed');
    expect(result.current.messages[1].errorCode).toBe('REPLY_TIMEOUT');
  });

  it('retry resends the same message once and does not duplicate the customer message', async () => {
    streamChat.mockReturnValueOnce(errorOnly()).mockReturnValueOnce(completeReply());
    const { result } = renderHook(() => useConversation());

    await act(async () => {
      await result.current.send('Slow?');
    });
    const failedId = result.current.messages[1].id;

    await act(async () => {
      await result.current.retry(failedId);
    });

    expect(streamChat).toHaveBeenCalledTimes(2);
    expect(streamChat.mock.calls[1][1]).toBe('Slow?');
    expect(result.current.messages.map((m) => m.sender)).toEqual(['customer', 'assistant']);
    expect(result.current.messages[1].status).toBe('complete');
    expect(result.current.messages[1].text).toBe('Fixed.');
  });

  it('never renders a raw error code in the page', async () => {
    streamChat.mockReturnValueOnce(errorOnly());
    render(<App />);

    await userEvent.type(screen.getByLabelText('Your question'), 'Slow?');
    await userEvent.click(screen.getByRole('button', { name: 'Send' }));

    await waitFor(() => expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument());
    expect(screen.queryByText(/REPLY_TIMEOUT/)).toBeNull();
    expect(screen.getByRole('alert')).toHaveTextContent('This is taking longer than expected.');
  });
});
