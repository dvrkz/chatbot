import { describe, expect, it } from 'vitest';
import { canRetry, friendlyMessage } from '../src/errors/friendlyMessages';
import type { ErrorCode } from '../src/types/chat';

const CODES: ErrorCode[] = [
  'VALIDATION_FAILED',
  'RATE_LIMITED',
  'REPLY_TIMEOUT',
  'UPSTREAM_UNAVAILABLE',
  'INTERNAL_ERROR',
];

describe('friendlyMessage', () => {
  it('has a message for every error code', () => {
    for (const code of CODES) {
      expect(friendlyMessage(code).length).toBeGreaterThan(0);
    }
  });

  it('never shows raw error text, codes, or stack-like lines', () => {
    for (const code of CODES) {
      const text = friendlyMessage(code);
      expect(text).not.toContain(code);
      expect(text).not.toMatch(/Exception|Error:|\bat \w|\bstack\b|https?:\/\//i);
    }
  });

  it('falls back to a generic retryable message when the code is missing', () => {
    expect(friendlyMessage(undefined)).toBe(friendlyMessage('UPSTREAM_UNAVAILABLE'));
  });
});

describe('canRetry', () => {
  it('offers retry for transient failures but not for invalid input', () => {
    expect(canRetry('REPLY_TIMEOUT')).toBe(true);
    expect(canRetry('UPSTREAM_UNAVAILABLE')).toBe(true);
    expect(canRetry('VALIDATION_FAILED')).toBe(false);
  });
});
