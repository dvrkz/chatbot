import { act, renderHook } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useConversation } from '../src/hooks/useConversation';
import * as chatApi from '../src/services/chatApi';

vi.mock('../src/services/chatApi', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../src/services/chatApi')>();
  return { ...actual, streamChat: vi.fn(), clearSession: vi.fn(() => Promise.resolve()) };
});

const streamChat = vi.mocked(chatApi.streamChat);

async function* reply(text: string) {
  yield { type: 'token', text } as const;
  yield { type: 'done' } as const;
}

describe('multi-turn conversation', () => {
  beforeEach(() => {
    streamChat.mockReset();
  });

  it('reuses one session ID across sends and keeps every turn in the list', async () => {
    streamChat.mockReturnValueOnce(reply('First answer.')).mockReturnValueOnce(reply('Second answer.'));
    const { result } = renderHook(() => useConversation());

    await act(async () => {
      await result.current.send('How do I set temperature?');
    });
    await act(async () => {
      await result.current.send('What range does it accept?');
    });

    const [firstSession] = streamChat.mock.calls[0];
    const [secondSession] = streamChat.mock.calls[1];
    expect(secondSession).toBe(firstSession);
    expect(result.current.messages.map((m) => m.text)).toEqual([
      'How do I set temperature?',
      'First answer.',
      'What range does it accept?',
      'Second answer.',
    ]);
  });
});
