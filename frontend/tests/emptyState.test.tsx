import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { App } from '../src/App';
import { STARTER_QUESTIONS } from '../src/content/starterQuestions';

describe('opening screen (US1, T012)', () => {
  it('shows the greeting, the subline, the heading, and four starter questions', () => {
    render(<App />);

    expect(screen.getByRole('heading', { name: 'Hi, how can we help?' })).toBeInTheDocument();
    expect(screen.getByText('Ask about the API, errors, or rate limits.')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Suggested questions' })).toBeInTheDocument();

    for (const question of STARTER_QUESTIONS) {
      expect(screen.getByRole('button', { name: question.text })).toBeInTheDocument();
    }
    expect(screen.getAllByRole('button', { name: /\?$/ })).toHaveLength(4);
  });

  it('uses exactly the four questions from FR-006, in order', () => {
    expect(STARTER_QUESTIONS.map((question) => question.text)).toEqual([
      'Why am I getting a 429 error?',
      'How do I stream a response?',
      'What counts toward my token limit?',
      'How do I choose a model?',
    ]);
  });
});
