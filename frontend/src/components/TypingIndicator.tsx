interface Props {
  /** Show the words "Looking into it" next to the bar. Off once text is already on screen. */
  withLabel?: boolean;
}

/**
 * The streaming indicator (FR-014, prd-ui-2 section 5.5): a short violet segment that moves along a
 * thin bar. It is static under reduced motion. Keeps role="status" and the name "Assistant is typing".
 */
export function TypingIndicator({ withLabel = true }: Props) {
  return (
    <span className="typing-row" role="status" aria-label="Assistant is typing">
      <span className="typing-bar" aria-hidden="true">
        <span className="typing-bar-segment" />
      </span>
      {withLabel && <span className="typing-label">Looking into it</span>}
    </span>
  );
}
