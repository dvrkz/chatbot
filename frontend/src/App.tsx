import { useEffect, useRef } from 'react';
import { EmptyState } from './components/EmptyState';
import { Header } from './components/Header';
import { JumpToLatest } from './components/JumpToLatest';
import { MessageInput } from './components/MessageInput';
import { MessageList } from './components/MessageList';
import { useConversation } from './hooks/useConversation';
import { useStickToBottom } from './hooks/useStickToBottom';

export function App() {
  const { messages, status, send, retry, reset } = useConversation();
  const { ref: scrollRef, pinned, onScroll, scrollToBottom } = useStickToBottom(messages);
  const composerRef = useRef<HTMLTextAreaElement>(null);
  const previousStatus = useRef(status);

  // Focus returns to the composer when a reply ends (FR-021).
  useEffect(() => {
    if (previousStatus.current === 'streaming' && status === 'idle') {
      composerRef.current?.focus();
    }
    previousStatus.current = status;
  }, [status]);

  const handleSend = (text: string) => {
    scrollToBottom();
    void send(text);
  };

  // New conversation clears the screen and returns focus to the composer (FR-003, FR-021).
  const handleReset = () => {
    reset();
    scrollToBottom();
    composerRef.current?.focus();
  };

  const streaming = status === 'streaming';

  return (
    <main className="app">
      <Header onReset={handleReset} canReset={messages.length > 0} />
      <div className="message-scroll" ref={scrollRef} onScroll={onScroll}>
        {messages.length === 0 ? (
          <EmptyState onSelect={handleSend} />
        ) : (
          <MessageList messages={messages} streaming={streaming} onRetry={(id) => void retry(id)} />
        )}
      </div>
      <div className="composer-region">
        {!pinned && messages.length > 0 && <JumpToLatest onClick={scrollToBottom} />}
        <MessageInput ref={composerRef} disabled={streaming} onSend={handleSend} />
      </div>
    </main>
  );
}
