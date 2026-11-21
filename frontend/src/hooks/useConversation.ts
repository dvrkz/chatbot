import { useCallback, useEffect, useRef, useState } from 'react';
import { MAX_MESSAGE_CHARS } from '../config';
import { ChatApiError, clearSession, streamChat } from '../services/chatApi';
import type { ErrorCode, Message } from '../types/chat';

function newId(): string {
  return crypto.randomUUID();
}

export interface Conversation {
  messages: Message[];
  status: 'idle' | 'streaming';
  send: (text: string) => Promise<void>;
  /** Resends the customer message before a failed or cut-short assistant reply. */
  retry: (assistantMessageId: string) => Promise<void>;
  /** Stops any reply in flight, clears the transcript, and starts a new session (US5). */
  reset: () => void;
}

/**
 * One conversation. The session ID is created once per conversation and reused for every send
 * until reset. Sending is blocked while a reply streams and for blank or over-length input.
 */
export function useConversation(): Conversation {
  const sessionRef = useRef<string | null>(null);
  if (sessionRef.current === null) {
    sessionRef.current = newId();
  }
  const [messages, setMessages] = useState<Message[]>([]);
  const [status, setStatus] = useState<'idle' | 'streaming'>('idle');
  const streamingRef = useRef(false);
  const abortRef = useRef<AbortController | null>(null);
  const messagesRef = useRef<Message[]>(messages);
  messagesRef.current = messages;

  // Stop any in-flight reply when the component unmounts.
  useEffect(() => () => abortRef.current?.abort(), []);

  const update = useCallback((id: string, patch: (m: Message) => Message) => {
    setMessages((prev) => prev.map((m) => (m.id === id ? patch(m) : m)));
  }, []);

  /** Streams a reply into an existing assistant message. Shared by send and retry. */
  const runStream = useCallback(
    async (assistantId: string, text: string) => {
      const controller = new AbortController();
      abortRef.current = controller;
      streamingRef.current = true;
      setStatus('streaming');

      let code: ErrorCode | undefined;
      let finished = false;
      try {
        for await (const event of streamChat(sessionRef.current as string, text, controller.signal)) {
          if (event.type === 'token') {
            update(assistantId, (m) => ({ ...m, text: m.text + event.text }));
          } else if (event.type === 'done') {
            finished = true;
            update(assistantId, (m) => ({ ...m, status: 'complete' }));
          } else {
            code = event.code;
          }
        }
      } catch (error) {
        code = error instanceof ChatApiError ? error.code : 'UPSTREAM_UNAVAILABLE';
      } finally {
        // An aborted stream belongs to a reset or an unmount, so the state is left alone.
        if (!controller.signal.aborted) {
          if (!finished) {
            // Partial text is kept and marked incomplete. Empty text is marked failed.
            update(assistantId, (m) => ({
              ...m,
              status: m.text.length > 0 ? 'incomplete' : 'failed',
              errorCode: code ?? 'UPSTREAM_UNAVAILABLE',
            }));
          }
          streamingRef.current = false;
          setStatus('idle');
        }
      }
    },
    [update],
  );

  const send = useCallback(
    async (raw: string) => {
      const text = raw.trim();
      if (text.length === 0 || text.length > MAX_MESSAGE_CHARS || streamingRef.current) {
        return;
      }
      const assistantId = newId();
      setMessages((prev) => [
        ...prev,
        { id: newId(), sender: 'customer', text, status: 'complete' },
        { id: assistantId, sender: 'assistant', text: '', status: 'streaming' },
      ]);
      await runStream(assistantId, text);
    },
    [runStream],
  );

  const retry = useCallback(
    async (assistantMessageId: string) => {
      if (streamingRef.current) {
        return;
      }
      const list = messagesRef.current;
      const index = list.findIndex((m) => m.id === assistantMessageId);
      const customer = index > 0 ? list[index - 1] : undefined;
      if (!customer || customer.sender !== 'customer') {
        return;
      }
      // The server removed the failed turn's customer message, so resending it does not duplicate it.
      update(assistantMessageId, (m) => ({
        ...m,
        text: '',
        status: 'streaming',
        errorCode: undefined,
      }));
      await runStream(assistantMessageId, customer.text);
    },
    [runStream, update],
  );

  const reset = useCallback(() => {
    abortRef.current?.abort();
    streamingRef.current = false;
    const previous = sessionRef.current;
    sessionRef.current = newId();
    setMessages([]);
    setStatus('idle');
    if (previous) {
      // Best effort: the old context is discarded on the server, and a failure changes nothing here.
      clearSession(previous).catch(() => undefined);
    }
  }, []);

  return { messages, status, send, retry, reset };
}
