import type { ReactNode } from 'react';
import { cx } from '../ui/cx';
import styles from './Markdown.module.css';

/**
 * Work items are markdown files written for an agent to consume. This renders
 * the subset that actually appears in a bean — headings, paragraphs, bullets and
 * task boxes — as plain elements. No markdown library, no HTML injection: the
 * text is only ever set as text, never as innerHTML.
 */
export function Markdown({ source }: { source: string }) {
  const lines = source.split('\n');
  const nodes: ReactNode[] = [];
  let list: Array<{ text: string; checked: boolean | null }> = [];
  let paragraph: string[] = [];

  const flushList = () => {
    if (list.length === 0) return;
    const items = list;
    list = [];
    nodes.push(
      <ul className={styles.list} key={`l${nodes.length}`}>
        {items.map((item, index) => (
          <li className={styles.item} key={index}>
            <span
              className={cx(styles.bullet, item.checked === true && styles.checked)}
              aria-hidden="true"
            >
              {item.checked === null ? '·' : item.checked ? '✓' : '○'}
            </span>
            <span className={item.checked === true ? styles.done : undefined}>{item.text}</span>
          </li>
        ))}
      </ul>,
    );
  };

  const flushParagraph = () => {
    if (paragraph.length === 0) return;
    const text = paragraph.join(' ');
    paragraph = [];
    nodes.push(
      <p className={styles.paragraph} key={`p${nodes.length}`}>
        {text}
      </p>,
    );
  };

  for (const line of lines) {
    const trimmed = line.trim();

    if (trimmed === '') {
      flushList();
      flushParagraph();
      continue;
    }

    const heading = /^#{1,6}\s+(.*)$/.exec(trimmed);
    if (heading?.[1]) {
      flushList();
      flushParagraph();
      nodes.push(
        <h3 className={styles.heading} key={`h${nodes.length}`}>
          {heading[1]}
        </h3>,
      );
      continue;
    }

    const task = /^[-*]\s+\[( |x|X)\]\s+(.*)$/.exec(trimmed);
    if (task?.[2] !== undefined) {
      flushParagraph();
      list.push({ text: task[2], checked: task[1]?.toLowerCase() === 'x' });
      continue;
    }

    const bullet = /^[-*]\s+(.*)$/.exec(trimmed);
    if (bullet?.[1]) {
      flushParagraph();
      list.push({ text: bullet[1], checked: null });
      continue;
    }

    paragraph.push(trimmed);
  }

  flushList();
  flushParagraph();

  return <div className={styles.prose}>{nodes}</div>;
}
