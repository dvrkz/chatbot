import { API_BASE_URL } from '../config';
import type { ErrorCode } from '../types/chat';
import { parseSse } from './sseParser';

/** Events yielded to the UI for one reply. Exactly one of done or error ends the stream. */
export type ChatStreamEvent =
  | { type: 'token'; text: string }
  | { type: 'done' }
  | { type: 'error'; code: ErrorCode };

/** A failure with a closed error code. The message is never shown to the customer. */
export class ChatApiError extends Error {
  readonly code: ErrorCode;

  constructor(code: ErrorCode) {
    super(code);
    this.name = 'ChatApiError';
    this.code = code;
  }
}

const KNOWN_CODES: ReadonlySet<string> = new Set<ErrorCode>([
  'VALIDATION_FAILED',
  'RATE_LIMITED',
  'REPLY_TIMEOUT',
  'UPSTREAM_UNAVAILABLE',
  'INTERNAL_ERROR',
]);

function toErrorCode(value: unknown): ErrorCode {
  return typeof value === 'string' && KNOWN_CODES.has(value) ? (value as ErrorCode) : 'INTERNAL_ERROR';
}

/** Reads the code from an RFC 9457 problem body. Anything unexpected maps to INTERNAL_ERROR. */
async function problemCode(response: Response): Promise<ErrorCode> {
  try {
    const body = (await response.json()) as { code?: unknown };
    return toErrorCode(body.code);
  } catch {
    return 'INTERNAL_ERROR';
  }
}

/**
 * Streams one reply. Pre-stream failures (validation, rate limit) throw ChatApiError with the
 * server's code. Stream failures are yielded as an error event. A stream that closes without a
 * terminal event is reported as UPSTREAM_UNAVAILABLE, and the caller keeps any partial text.
 */
export async function* streamChat(
  sessionId: string,
  message: string,
  signal?: AbortSignal,
): AsyncGenerator<ChatStreamEvent> {
  const response = await fetch(`${API_BASE_URL}/api/chat/stream`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream' },
    body: JSON.stringify({ sessionId, message }),
    signal,
  });

  if (!response.ok) {
    throw new ChatApiError(await problemCode(response));
  }
  if (!response.body) {
    throw new ChatApiError('UPSTREAM_UNAVAILABLE');
  }

  let terminal = false;
  for await (const event of parseSse(response.body)) {
    if (event.event === 'token') {
      const payload = parsePayload(event.data);
      if (typeof payload.text === 'string' && payload.text.length > 0) {
        yield { type: 'token', text: payload.text };
      }
    } else if (event.event === 'done') {
      terminal = true;
      yield { type: 'done' };
      return;
    } else if (event.event === 'error') {
      terminal = true;
      yield { type: 'error', code: toErrorCode(parsePayload(event.data).code) };
      return;
    }
  }

  if (!terminal) {
    throw new ChatApiError('UPSTREAM_UNAVAILABLE');
  }
}

/** Throws UPSTREAM_UNAVAILABLE when the data is not valid JSON, per the parsing rules. */
function parsePayload(data: string): Record<string, unknown> {
  try {
    const parsed: unknown = JSON.parse(data);
    if (parsed === null || typeof parsed !== 'object') {
      throw new Error('not an object');
    }
    return parsed as Record<string, unknown>;
  } catch {
    throw new ChatApiError('UPSTREAM_UNAVAILABLE');
  }
}

/** Clears the server-side context for a session. Used by "New conversation" (US5). */
export async function clearSession(sessionId: string): Promise<void> {
  await fetch(`${API_BASE_URL}/api/chat/sessions/${encodeURIComponent(sessionId)}`, {
    method: 'DELETE',
  });
}
