interface Props {
  onReset: () => void;
  /** Unavailable while the conversation is empty (FR-022). */
  disabled?: boolean;
}

/** Starts a fresh conversation (FR-003). A native button, so it is keyboard accessible. */
export function NewConversationButton({ onReset, disabled = false }: Props) {
  return (
    <button type="button" className="new-conversation" onClick={onReset} disabled={disabled}>
      <svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
        <path d="M12 5v14M5 12h14" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" />
      </svg>
      <span>New conversation</span>
    </button>
  );
}
