import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useConversation } from '../src/hooks/useConversation';
import * as chatApi from '../src/services/chatApi';

vi.mock('../src/services/chatApi', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../src/services/chatApi')>();
  return { ...actual, streamChat: vi.fn() };
});

const streamChat = vi.mocked(chatApi.streamChat);

async function* events(...items: chatApi.ChatStreamEvent[]) {
  for (const item of items) {
    yield item;
  }
}

describe('useConversation', () => {
  beforeEach(() => {
    streamChat.mockReset();
  });

  it('streams tokens into the assistant message and completes', async () => {
    streamChat.mockReturnValue(
      events({ type: 'token', text: 'Hello ' }, { type: 'token', text: 'there' }, { type: 'done' }),
    );
    const { result } = renderHook(() => useConversation());

    await act(async () => {
      await result.current.send('Hi');
    });

    expect(result.current.messages.map((m) => m.text)).toEqual(['Hi', 'Hello there']);
    expect(result.current.messages[1].status).toBe('complete');
    expect(result.current.status).toBe('idle');
  });

  it('blocks a second send while a reply is streaming', async () => {
    let release: () => void = () => {};
    streamChat.mockImplementation(async function* () {
      await new Promise<void>((resolve) => {
        release = resolve;
      });
      yield { type: 'done' } as const;
    });
    const { result } = renderHook(() => useConversation());

    let first: Promise<void> = Promise.resolve();
    act(() => {
      first = result.current.send('one');
    });
    await act(async () => {
      await result.current.send('two');
    });

    expect(streamChat).toHaveBeenCalledTimes(1);
    release();
    await act(async () => {
      await first;
    });
  });

  it('does not send blank input', async () => {
    const { result } = renderHook(() => useConversation());

    await act(async () => {
      await result.current.send('   ');
    });

    expect(streamChat).not.toHaveBeenCalled();
    expect(result.current.messages).toEqual([]);
  });

  it('marks a failed reply as failed with no text', async () => {
    streamChat.mockImplementation(() => {
      throw new chatApi.ChatApiError('UPSTREAM_UNAVAILABLE');
    });
    const { result } = renderHook(() => useConversation());

    await act(async () => {
      await result.current.send('Why 401?');
    });

    await waitFor(() => expect(result.current.status).toBe('idle'));
    expect(result.current.messages[1].status).toBe('failed');
    expect(result.current.messages[1].errorCode).toBe('UPSTREAM_UNAVAILABLE');
  });
});
