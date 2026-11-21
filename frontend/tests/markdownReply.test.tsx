import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { MessageList } from '../src/components/MessageList';
import type { Message } from '../src/types/chat';

const reply = [
  '- first item',
  '- second item',
  '',
  'Set `temperature` to `0.2`.',
  '',
  '> Values above 1 are unusual.',
  '',
  '| Field | Meaning |',
  '| --- | --- |',
  '| model | Model name |',
  '',
  'See [the docs](https://docs.example.test).',
  '',
  '<script>alert(1)</script>',
].join('\n');

const messages: Message[] = [{ id: 'reply-1', sender: 'assistant', status: 'complete', text: reply }];

describe('markdown reply presentation (US2, T018)', () => {
  it('renders lists, inline code, quotes, and links with the required attributes', () => {
    const { container } = render(<MessageList messages={messages} streaming={false} onRetry={() => undefined} />);

    expect(screen.getByText('first item').closest('ul')).not.toBeNull();
    expect(screen.getByText('temperature').tagName).toBe('CODE');
    expect(container.querySelector('blockquote')).not.toBeNull();

    const link = screen.getByRole('link', { name: 'the docs' });
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');
  });

  // Known gap: react-markdown needs remark-gfm to parse GFM tables (FR-009, UI-FR-4). The dependency
  // install was not permitted, so this test fails until remark-gfm is added (see measurements.md).
  it('renders a GFM table inside a horizontally scrolling wrapper (FR-009, FR-012)', () => {
    const { container } = render(<MessageList messages={messages} streaming={false} onRetry={() => undefined} />);

    expect(container.querySelector('.table-scroll table')).not.toBeNull();
  });

  it('never creates a script element from reply text', () => {
    const { container } = render(<MessageList messages={messages} streaming={false} onRetry={() => undefined} />);
    expect(container.querySelector('script')).toBeNull();
  });
});
