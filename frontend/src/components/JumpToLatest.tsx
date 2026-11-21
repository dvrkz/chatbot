interface Props {
  onClick: () => void;
}

/** Shown when the customer has scrolled away from the newest text (UI-FR-7, FR-015). */
export function JumpToLatest({ onClick }: Props) {
  return (
    <button type="button" className="jump-pill" onClick={onClick}>
      Jump to latest
    </button>
  );
}
