import { act, fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { MessageList } from '../src/components/MessageList';
import { copyText } from '../src/utils/copyText';
import type { Message } from '../src/types/chat';

vi.mock('../src/utils/copyText', () => ({ copyText: vi.fn(() => Promise.resolve(true)) }));

const copy = vi.mocked(copyText);

const messages: Message[] = [
  {
    id: 'reply-1',
    sender: 'assistant',
    status: 'complete',
    text: 'Run this:\n\n```bash\ncurl https://api.example.test/v1/models\n```',
  },
];

describe('code copy (US2, T017)', () => {
  afterEach(() => {
    vi.useRealTimers();
    copy.mockClear();
  });

  it('copies the code without fence markers and confirms with Copied', async () => {
    render(<MessageList messages={messages} streaming={false} onRetry={() => undefined} />);

    await userEvent.click(screen.getByRole('button', { name: 'Copy' }));

    expect(copy).toHaveBeenCalledTimes(1);
    const copied = copy.mock.calls[0][0];
    expect(copied).toContain('curl https://api.example.test/v1/models');
    expect(copied).not.toContain('```');
    expect(await screen.findByRole('button', { name: 'Copied' })).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('Copied');
  });

  it('reverts the label to Copy after two seconds', async () => {
    vi.useFakeTimers();
    render(<MessageList messages={messages} streaming={false} onRetry={() => undefined} />);

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Copy' }));
    });
    expect(screen.getByRole('button', { name: 'Copied' })).toBeInTheDocument();

    act(() => {
      vi.advanceTimersByTime(2000);
    });
    expect(screen.getByRole('button', { name: 'Copy' })).toBeInTheDocument();
  });
});
