import { isValidElement, useEffect, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import { copyText } from '../utils/copyText';

/** Collects the plain text of a React node tree, so the Copy action copies exactly the code. */
function textOf(node: ReactNode): string {
  if (node === null || node === undefined || typeof node === 'boolean') {
    return '';
  }
  if (typeof node === 'string' || typeof node === 'number') {
    return String(node);
  }
  if (Array.isArray(node)) {
    return node.map(textOf).join('');
  }
  if (isValidElement<{ children?: ReactNode }>(node)) {
    return textOf(node.props.children);
  }
  return '';
}

interface Props {
  /** The children of the markdown `pre` element, which holds the code element. */
  children: ReactNode;
}

/**
 * A fenced code block with a Copy action (UI-FR-5, FR-010). After a copy, the label reads "Copied"
 * for two seconds, and the change is announced through a polite status region.
 */
export function CodeBlock({ children }: Props) {
  const [copied, setCopied] = useState(false);
  const timer = useRef<number | undefined>(undefined);

  useEffect(
    () => () => {
      if (timer.current !== undefined) {
        window.clearTimeout(timer.current);
      }
    },
    [],
  );

  const onCopy = async () => {
    const ok = await copyText(textOf(children).replace(/\n$/, ''));
    if (!ok) {
      return;
    }
    setCopied(true);
    if (timer.current !== undefined) {
      window.clearTimeout(timer.current);
    }
    timer.current = window.setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="code-wrap">
      <pre className="code-block">{children}</pre>
      <button type="button" className="copy-pill" onClick={() => void onCopy()}>
        {copied ? 'Copied' : 'Copy'}
      </button>
      <span className="visually-hidden" role="status">
        {copied ? 'Copied' : ''}
      </span>
    </div>
  );
}
