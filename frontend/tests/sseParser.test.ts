import { describe, expect, it } from 'vitest';
import { parseSse } from '../src/services/sseParser';

function streamOf(chunks: Uint8Array[]): ReadableStream<Uint8Array> {
  return new ReadableStream<Uint8Array>({
    start(controller) {
      for (const chunk of chunks) {
        controller.enqueue(chunk);
      }
      controller.close();
    },
  });
}

async function collect(chunks: Uint8Array[]) {
  const events = [];
  for await (const event of parseSse(streamOf(chunks))) {
    events.push(event);
  }
  return events;
}

const enc = new TextEncoder();

describe('parseSse', () => {
  it('keeps token order across blocks', async () => {
    const events = await collect([
      enc.encode('event: token\ndata: {"text":"A"}\n\nevent: token\ndata: {"text":"B"}\n\n'),
    ]);
    expect(events.map((e) => e.data)).toEqual(['{"text":"A"}', '{"text":"B"}']);
  });

  it('decodes a multi-byte UTF-8 character split across chunks', async () => {
    const bytes = enc.encode('event: token\ndata: {"text":"café"}\n\n');
    const cut = bytes.indexOf(0xc3) + 1; // split between the two bytes of "é"
    const events = await collect([bytes.slice(0, cut), bytes.slice(cut)]);
    expect(events).toEqual([{ event: 'token', data: '{"text":"café"}' }]);
  });

  it('ignores comment lines', async () => {
    const events = await collect([enc.encode(': keep-alive\nevent: done\ndata: {}\n\n')]);
    expect(events).toEqual([{ event: 'done', data: '{}' }]);
  });

  it('passes unknown events through for the caller to ignore', async () => {
    const events = await collect([enc.encode('event: mystery\ndata: {}\n\n')]);
    expect(events).toEqual([{ event: 'mystery', data: '{}' }]);
  });

  it('does not emit a trailing block without a blank line', async () => {
    const events = await collect([enc.encode('event: token\ndata: {"text":"cut"}')]);
    expect(events).toEqual([]);
  });

  it('handles CRLF line endings', async () => {
    const events = await collect([enc.encode('event: done\r\ndata: {}\r\n\r\n')]);
    expect(events).toEqual([{ event: 'done', data: '{}' }]);
  });
});
