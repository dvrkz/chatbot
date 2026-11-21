import { canRetry, friendlyMessage } from '../errors/friendlyMessages';
import type { ErrorCode } from '../types/chat';

interface Props {
  code: ErrorCode | undefined;
  /** True when the reply was cut short after some text had already arrived. */
  partial: boolean;
  onRetry: () => void;
}

/**
 * The failure notice inside a failed or cut-off reply (UI-FR-10). Shows the friendly text and a
 * Try again action. Never shows the error code or any raw text (Principle V).
 */
export function ErrorBanner({ code, partial, onRetry }: Props) {
  return (
    <div className="notice">
      <p role="alert">
        {partial && <span>Reply was cut short. </span>}
        {friendlyMessage(code)}
      </p>
      {canRetry(code) && (
        <button type="button" className="pill-outline-coral" onClick={onRetry}>
          Try again
        </button>
      )}
    </div>
  );
}
