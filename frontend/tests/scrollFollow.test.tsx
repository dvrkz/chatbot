import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { App } from '../src/App';
import * as chatApi from '../src/services/chatApi';

vi.mock('../src/services/chatApi', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../src/services/chatApi')>();
  return { ...actual, streamChat: vi.fn(), clearSession: vi.fn(() => Promise.resolve()) };
});

const streamChat = vi.mocked(chatApi.streamChat);

/** A reply whose second half is released by the test, so the test controls the moment of growth. */
let release: () => void = () => undefined;
function controlledReply() {
  const gate = new Promise<void>((resolve) => {
    release = resolve;
  });
  return (async function* () {
    yield { type: 'token', text: 'First part. ' } as const;
    await gate;
    yield { type: 'token', text: 'Second part.' } as const;
    yield { type: 'done' } as const;
  })();
}

/** jsdom has no layout, so the test supplies the scroll metrics. */
function setScrollMetrics(el: HTMLElement, metrics: { scrollHeight: number; clientHeight: number; scrollTop: number }) {
  Object.defineProperty(el, 'scrollHeight', { configurable: true, get: () => metrics.scrollHeight });
  Object.defineProperty(el, 'clientHeight', { configurable: true, get: () => metrics.clientHeight });
  Object.defineProperty(el, 'scrollTop', {
    configurable: true,
    get: () => metrics.scrollTop,
    set: (value: number) => {
      metrics.scrollTop = value;
    },
  });
}

async function sendLongQuestion() {
  await userEvent.type(screen.getByLabelText('Your question'), 'Long question');
  await userEvent.click(screen.getByRole('button', { name: 'Send' }));
  await screen.findByText('First part.');
}

describe('scroll follow while streaming (US3, T023)', () => {
  beforeEach(() => {
    streamChat.mockReset();
    streamChat.mockImplementation(() => controlledReply());
  });

  it('follows the newest text while the customer is at the bottom (FR-015)', async () => {
    const { container } = render(<App />);
    const scroll = container.querySelector('.message-scroll') as HTMLElement;
    const metrics = { scrollHeight: 1000, clientHeight: 400, scrollTop: 600 };
    setScrollMetrics(scroll, metrics);

    await sendLongQuestion();
    metrics.scrollHeight = 1200;
    release();
    await screen.findByText(/Second part\./);

    expect(metrics.scrollTop).toBe(1200);
  });

  it('holds still and offers Jump to latest when the customer has scrolled up (FR-015, FR-017)', async () => {
    const { container } = render(<App />);
    const scroll = container.querySelector('.message-scroll') as HTMLElement;
    const metrics = { scrollHeight: 1000, clientHeight: 400, scrollTop: 600 };
    setScrollMetrics(scroll, metrics);

    await sendLongQuestion();
    metrics.scrollTop = 100;
    fireEvent.scroll(scroll);
    metrics.scrollHeight = 1200;
    release();
    await screen.findByText(/Second part\./);

    expect(metrics.scrollTop).toBe(100);
    expect(screen.getByRole('button', { name: 'Jump to latest' })).toBeInTheDocument();
  });

  it('returns to the newest text and hides the pill when Jump to latest is used', async () => {
    const { container } = render(<App />);
    const scroll = container.querySelector('.message-scroll') as HTMLElement;
    const metrics = { scrollHeight: 1000, clientHeight: 400, scrollTop: 600 };
    setScrollMetrics(scroll, metrics);

    await sendLongQuestion();
    metrics.scrollTop = 100;
    fireEvent.scroll(scroll);
    expect(screen.getByRole('button', { name: 'Jump to latest' })).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Jump to latest' }));
    expect(metrics.scrollTop).toBe(1000);
    await waitFor(() => expect(screen.queryByRole('button', { name: 'Jump to latest' })).toBeNull());
    release();
  });
});
