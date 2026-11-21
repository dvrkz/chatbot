/** Closed types from data-model.md. Mirrors the backend ErrorCode set. */

export type Sender = 'customer' | 'assistant';

export type MessageStatus = 'complete' | 'streaming' | 'failed' | 'incomplete';

export type ErrorCode =
  | 'VALIDATION_FAILED'
  | 'RATE_LIMITED'
  | 'REPLY_TIMEOUT'
  | 'UPSTREAM_UNAVAILABLE'
  | 'INTERNAL_ERROR';

export interface Message {
  id: string;
  sender: Sender;
  text: string;
  status: MessageStatus;
  /** Present only when status is 'failed' or 'incomplete'. */
  errorCode?: ErrorCode;
}
