import { useCallback, useLayoutEffect, useRef, useState } from 'react';

/** Within this many pixels of the bottom, the list counts as "at the bottom" (research R4). */
export const STICK_THRESHOLD_PX = 48;

/**
 * Scroll-follow for a streaming list (FR-015 to FR-017).
 * While pinned to the bottom, the list follows each change of `changeKey`. Once the customer scrolls
 * up, the list does not move, and `pinned` becomes false so the caller can show "Jump to latest".
 */
export function useStickToBottom<T>(changeKey: T) {
  const ref = useRef<HTMLDivElement>(null);
  const pinnedRef = useRef(true);
  const [pinned, setPinned] = useState(true);

  const onScroll = useCallback(() => {
    const el = ref.current;
    if (!el) {
      return;
    }
    const nearBottom = el.scrollHeight - el.scrollTop - el.clientHeight <= STICK_THRESHOLD_PX;
    pinnedRef.current = nearBottom;
    setPinned(nearBottom);
  }, []);

  const scrollToBottom = useCallback(() => {
    const el = ref.current;
    if (el) {
      el.scrollTop = el.scrollHeight;
    }
    pinnedRef.current = true;
    setPinned(true);
  }, []);

  useLayoutEffect(() => {
    const el = ref.current;
    if (el && pinnedRef.current) {
      el.scrollTop = el.scrollHeight;
    }
  }, [changeKey]);

  return { ref, pinned, onScroll, scrollToBottom };
}
