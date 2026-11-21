/**
 * The four fixed opening questions (FR-006, data-model section 3). Each one is inside the Principle IV
 * scope. Choosing one sends it exactly as if the customer had typed it (FR-007).
 */
export const STARTER_QUESTIONS = [
  { id: 'rate-limit-429', text: 'Why am I getting a 429 error?' },
  { id: 'stream-response', text: 'How do I stream a response?' },
  { id: 'token-limit', text: 'What counts toward my token limit?' },
  { id: 'choose-model', text: 'How do I choose a model?' },
] as const;
