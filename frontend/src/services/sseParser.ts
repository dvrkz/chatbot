/** One parsed Server-Sent Event block. Unknown event names are passed through unchanged. */
export interface SseEvent {
  event: string;
  data: string;
}

const DECODER_OPTIONS = { stream: true } as const;

/**
 * Parses a text/event-stream body into events (contracts/chat-stream-events.md).
 * - Splits on blank lines.
 * - A UTF-8 character split across network chunks is decoded correctly (TextDecoder stream mode).
 * - Ignores comment lines that start with ":" and blocks with no data.
 * - Ignores id: and retry: fields. Nothing auto-reconnects.
 * - A trailing block with no blank line is not emitted. The caller treats that as a dropped connection.
 */
export async function* parseSse(body: ReadableStream<Uint8Array>): AsyncGenerator<SseEvent> {
  const reader = body.getReader();
  const decoder = new TextDecoder('utf-8');
  let buffer = '';

  for (;;) {
    const { value, done } = await reader.read();
    if (done) {
      break;
    }
    buffer += decoder.decode(value, DECODER_OPTIONS);
    buffer = buffer.replace(/\r\n/g, '\n');

    let boundary = buffer.indexOf('\n\n');
    while (boundary !== -1) {
      const block = buffer.slice(0, boundary);
      buffer = buffer.slice(boundary + 2);
      const event = parseBlock(block);
      if (event) {
        yield event;
      }
      boundary = buffer.indexOf('\n\n');
    }
  }
  buffer += decoder.decode();
}

function parseBlock(block: string): SseEvent | null {
  let event = 'message';
  const dataLines: string[] = [];
  for (const line of block.split('\n')) {
    if (line.length === 0 || line.startsWith(':')) {
      continue;
    }
    const colon = line.indexOf(':');
    const field = colon === -1 ? line : line.slice(0, colon);
    let value = colon === -1 ? '' : line.slice(colon + 1);
    if (value.startsWith(' ')) {
      value = value.slice(1);
    }
    if (field === 'event') {
      event = value;
    } else if (field === 'data') {
      dataLines.push(value);
    }
  }
  if (dataLines.length === 0) {
    return null;
  }
  return { event, data: dataLines.join('\n') };
}
