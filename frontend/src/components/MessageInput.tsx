import { forwardRef, useImperativeHandle, useLayoutEffect, useRef, useState } from 'react';
import type { KeyboardEvent } from 'react';
import { MAX_MESSAGE_CHARS } from '../config';

interface Props {
  /** Disables Send only. The textarea stays editable while a reply streams (FR-019). */
  disabled: boolean;
  onSend: (text: string) => void;
}

/** Lines the textarea may grow to before it scrolls (FR-018). */
const MAX_LINES = 8;

const numberFormat = new Intl.NumberFormat('en-US');

/**
 * The composer (UI-FR-8, UI-FR-9): an auto-growing textarea, a length counter near the limit, and a
 * circular Send button. Blank and over-length messages are refused with a plain note (FR-005).
 */
export const MessageInput = forwardRef<HTMLTextAreaElement, Props>(function MessageInput(
  { disabled, onSend },
  forwardedRef,
) {
  const [value, setValue] = useState('');
  const [note, setNote] = useState<string | null>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useImperativeHandle(forwardedRef, () => textareaRef.current as HTMLTextAreaElement);

  // Auto-grow: size to the content, capped at MAX_LINES, then scroll (research R5).
  useLayoutEffect(() => {
    const el = textareaRef.current;
    if (!el) {
      return;
    }
    el.style.height = 'auto';
    const lineHeight = parseFloat(getComputedStyle(el).lineHeight) || 24;
    const max = lineHeight * MAX_LINES;
    el.style.height = `${Math.min(el.scrollHeight, max)}px`;
    el.style.overflowY = el.scrollHeight > max ? 'auto' : 'hidden';
  }, [value]);

  const submit = () => {
    if (disabled) {
      return;
    }
    const trimmed = value.trim();
    if (trimmed.length === 0) {
      setNote('A message is required.');
      return;
    }
    if (trimmed.length > MAX_MESSAGE_CHARS) {
      setNote(`Messages can be at most ${MAX_MESSAGE_CHARS} characters.`);
      return;
    }
    setNote(null);
    setValue('');
    onSend(trimmed);
  };

  const onKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      submit();
    }
  };

  const threshold = Math.floor(MAX_MESSAGE_CHARS * 0.8);
  const showCounter = value.length >= threshold;
  const overLimit = value.length > MAX_MESSAGE_CHARS;

  return (
    <form
      className="composer"
      onSubmit={(event) => {
        event.preventDefault();
        submit();
      }}
    >
      <label htmlFor="message" className="visually-hidden">
        Your question
      </label>
      <div className="composer-row">
        <textarea
          id="message"
          ref={textareaRef}
          rows={1}
          value={value}
          placeholder="Ask about the API, errors, or rate limits"
          onChange={(event) => setValue(event.target.value)}
          onKeyDown={onKeyDown}
        />
        <button type="submit" className="send-button" aria-label="Send" disabled={disabled}>
          <svg width="20" height="20" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <path d="M3 11.5 21 3l-8.5 18-2.2-7.3L3 11.5Z" fill="currentColor" />
          </svg>
        </button>
      </div>
      <div className="composer-meta">
        <p className="composer-hint">Enter to send, Shift+Enter for a new line</p>
        {showCounter && (
          <span className={overLimit ? 'counter counter-over' : 'counter'}>
            {`${numberFormat.format(value.length)} of ${numberFormat.format(MAX_MESSAGE_CHARS)}`}
          </span>
        )}
      </div>
      {note && (
        <p className="note" role="alert">
          {note}
        </p>
      )}
    </form>
  );
});
