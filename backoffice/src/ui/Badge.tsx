import type { ReactNode } from 'react';
import { cx } from './cx';
import styles from './Badge.module.css';

export type BadgeTone = 'neutral' | 'accent' | 'good' | 'warn' | 'danger' | 'info' | 'spend';

export interface BadgeProps {
    tone?: BadgeTone;
    /** Adds a status dot so the state is not carried by colour alone. */
    dot?: boolean;
    children: ReactNode;
    className?: string;
}

export function Badge({ tone = 'neutral', dot = false, children, className }: BadgeProps) {
    return (
        <span className={cx(styles.badge, styles[tone], className)}>
            {dot && <span className={styles.dot} aria-hidden="true" />}
            {children}
        </span>
    );
}
