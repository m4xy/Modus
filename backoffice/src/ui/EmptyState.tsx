import type { ReactNode } from 'react';
import styles from './EmptyState.module.css';

export interface EmptyStateProps {
  title: string;
  description: string;
  /** Small decorative glyph — never the only carrier of meaning. */
  mark?: ReactNode;
  action?: ReactNode;
}

export function EmptyState({ title, description, mark, action }: EmptyStateProps) {
  return (
    <div className={styles.empty}>
      <span className={styles.mark} aria-hidden="true">
        {mark ?? '—'}
      </span>
      <p className={styles.title}>{title}</p>
      <p className={styles.description}>{description}</p>
      {action && <div className={styles.action}>{action}</div>}
    </div>
  );
}
