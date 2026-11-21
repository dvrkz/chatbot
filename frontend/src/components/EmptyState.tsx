import { STARTER_QUESTIONS } from '../content/starterQuestions';

interface Props {
  /** Sends the chosen question exactly as if typed (FR-007). */
  onSelect: (text: string) => void;
}

/** Opening screen with no messages: greeting, subline, and four starter questions (FR-006). */
export function EmptyState({ onSelect }: Props) {
  return (
    <section className="empty-state" aria-labelledby="empty-title">
      <h2 id="empty-title">Hi, how can we help?</h2>
      <p className="subline">Ask about the API, errors, or rate limits.</p>
      <h3 className="section-label">Suggested questions</h3>
      <ul className="starter-row">
        {STARTER_QUESTIONS.map((question) => (
          <li key={question.id}>
            <button type="button" className="starter-card" onClick={() => onSelect(question.text)}>
              {question.text}
            </button>
          </li>
        ))}
      </ul>
    </section>
  );
}
