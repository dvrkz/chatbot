import { act, renderHook } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useConversation } from '../src/hooks/useConversation';
import * as chatApi from '../src/services/chatApi';

vi.mock('../src/services/chatApi', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../src/services/chatApi')>();
  return { ...actual, streamChat: vi.fn(), clearSession: vi.fn(() => Promise.resolve()) };
});

const streamChat = vi.mocked(chatApi.streamChat);
const clearSession = vi.mocked(chatApi.clearSession);

async function* reply(text: string) {
  yield { type: 'token', text } as const;
  yield { type: 'done' } as const;
}

describe('new conversation', () => {
  beforeEach(() => {
    streamChat.mockReset();
    clearSession.mockClear();
  });

  it('clears the transcript, discards the old session, and starts a new session ID', async () => {
    streamChat.mockReturnValueOnce(reply('Old topic.')).mockReturnValueOnce(reply('Fresh start.'));
    const { result } = renderHook(() => useConversation());

    await act(async () => {
      await result.current.send('Question about 429');
    });
    const oldSession = streamChat.mock.calls[0][0];

    act(() => {
      result.current.reset();
    });
    expect(result.current.messages).toEqual([]);
    expect(clearSession).toHaveBeenCalledWith(oldSession);

    await act(async () => {
      await result.current.send('Follow-up');
    });
    expect(streamChat.mock.calls[1][0]).not.toBe(oldSession);
  });

  it('aborts an in-flight reply when a new conversation starts', async () => {
    let capturedSignal: AbortSignal | undefined;
    streamChat.mockImplementation((_session, _text, signal) => {
      capturedSignal = signal;
      return (async function* () {
        await new Promise<void>(() => undefined); // never resolves: the reply is stuck
        yield { type: 'done' } as const;
      })();
    });
    const { result } = renderHook(() => useConversation());

    act(() => {
      void result.current.send('Long question');
    });
    act(() => {
      result.current.reset();
    });

    expect(capturedSignal?.aborted).toBe(true);
  });
});
