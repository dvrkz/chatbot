import ReactMarkdown from 'react-markdown';
import type { Components } from 'react-markdown';
import type { Message } from '../types/chat';
import { CodeBlock } from './CodeBlock';
import { ErrorBanner } from './ErrorBanner';
import { TypingIndicator } from './TypingIndicator';

interface Props {
  messages: Message[];
  /** True while a reply streams. Sets aria-busy so announcements wait for the complete reply (R10). */
  streaming: boolean;
  onRetry: (assistantMessageId: string) => void;
}

/**
 * Markdown presentation for assistant replies (UI-FR-4 to UI-FR-6, research R7). Raw HTML is never
 * rendered: this map adds no raw-HTML plugin, so react-markdown keeps escaping it.
 */
const replyComponents: Components = {
  a: ({ href, children }) => (
    <a href={href} target="_blank" rel="noopener noreferrer">
      {children}
    </a>
  ),
  pre: ({ children }) => <CodeBlock>{children}</CodeBlock>,
  table: ({ children }) => (
    <div className="table-scroll">
      <table>{children}</table>
    </div>
  ),
  h1: ({ children }) => <h3 className="reply-heading">{children}</h3>,
  h2: ({ children }) => <h3 className="reply-heading">{children}</h3>,
  h3: ({ children }) => <h3 className="reply-heading">{children}</h3>,
};

/**
 * The conversation transcript. Customer messages are violet bubbles. Assistant replies are white
 * cards with markdown content, a streaming indicator, and an error notice when needed (prd-ui-2 5.5).
 */
export function MessageList({ messages, streaming, onRetry }: Props) {
  return (
    <ol className="message-list" aria-live="polite" aria-busy={streaming}>
      {messages.map((message) => (
        <li key={message.id} className={`message message-${message.sender}`}>
          {message.sender === 'customer' ? (
            <p>{message.text}</p>
          ) : (
            <article className="message-assistant">
              {message.text.length > 0 ? (
                <div className="reply">
                  <ReactMarkdown components={replyComponents}>{message.text}</ReactMarkdown>
                </div>
              ) : null}
              {message.status === 'streaming' && (
                <TypingIndicator withLabel={message.text.length === 0} />
              )}
              {(message.status === 'failed' || message.status === 'incomplete') && (
                <ErrorBanner
                  code={message.errorCode}
                  partial={message.status === 'incomplete'}
                  onRetry={() => onRetry(message.id)}
                />
              )}
            </article>
          )}
        </li>
      ))}
    </ol>
  );
}
