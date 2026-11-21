/**
 * Copies text to the clipboard. Uses the Clipboard API when it exists (secure contexts only), and
 * otherwise falls back to a temporary textarea and execCommand. No dependency (research R6).
 * Resolves true when the text was copied, false otherwise.
 */
export async function copyText(text: string): Promise<boolean> {
  if (typeof navigator !== 'undefined' && navigator.clipboard?.writeText) {
    try {
      await navigator.clipboard.writeText(text);
      return true;
    } catch {
      // Fall through to the textarea fallback (for example, permission denied).
    }
  }
  if (typeof document === 'undefined' || !document.body) {
    return false;
  }
  const area = document.createElement('textarea');
  area.value = text;
  area.setAttribute('readonly', '');
  area.style.position = 'fixed';
  area.style.opacity = '0';
  area.style.left = '-9999px';
  document.body.appendChild(area);
  area.select();
  let copied = false;
  try {
    copied = document.execCommand('copy');
  } catch {
    copied = false;
  } finally {
    document.body.removeChild(area);
  }
  return copied;
}
