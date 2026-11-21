import type { ErrorCode } from '../types/chat';

/**
 * Customer-facing text for each failure. The UI owns all of it: the backend never sends prose, and
 * error codes are never shown (constitution Principle V, FR-011).
 */
const MESSAGES: Record<ErrorCode, string> = {
  VALIDATION_FAILED: 'Please check your message and try again.',
  RATE_LIMITED: "You're sending messages quickly. Please wait a moment and try again.",
  REPLY_TIMEOUT: 'This is taking longer than expected. Please try again.',
  UPSTREAM_UNAVAILABLE: "We couldn't get a reply right now. Please try again shortly.",
  INTERNAL_ERROR: 'Something went wrong on our side. Please try again.',
};

export function friendlyMessage(code: ErrorCode | undefined): string {
  return MESSAGES[code ?? 'UPSTREAM_UNAVAILABLE'];
}

/** Retry only makes sense when the same message might succeed later. */
export function canRetry(code: ErrorCode | undefined): boolean {
  return code !== 'VALIDATION_FAILED';
}
