import { NewConversationButton } from './NewConversationButton';

interface Props {
  onReset: () => void;
  /** False while the conversation is empty, which disables New conversation (FR-022). */
  canReset: boolean;
}

/** Sticky header: assistant mark, title, and the New conversation action (UI-FR-2). */
export function Header({ onReset, canReset }: Props) {
  return (
    <header className="app-header">
      <div className="brand">
        <span className="mark" aria-hidden="true">
          <svg width="18" height="18" viewBox="0 0 24 24" focusable="false">
            <path
              d="M16.5 8.2A6.5 6.5 0 1 0 16.5 15.8"
              fill="none"
              stroke="currentColor"
              strokeWidth="2.6"
              strokeLinecap="round"
            />
          </svg>
        </span>
        <h1>Caramel AI Tech Support</h1>
      </div>
      <NewConversationButton onReset={onReset} disabled={!canReset} />
    </header>
  );
}
