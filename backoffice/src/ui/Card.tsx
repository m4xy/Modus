import type { ReactNode } from 'react';
import { cx } from './cx';
import styles from './Card.module.css';

export interface CardProps {
    children: ReactNode;
    className?: string;
    /** Drops the elevation for cards that sit inside another surface. */
    flush?: boolean;
    as?: 'div' | 'section' | 'article';
    'aria-labelledby'?: string;
}

export function Card({ children, className, flush = false, as = 'section', ...rest }: CardProps) {
    const Tag = as;
    return (
        <Tag className={cx(styles.card, flush && styles.flush, className)} {...rest}>
            {children}
        </Tag>
    );
}

export interface CardHeaderProps {
    title: ReactNode;
    eyebrow?: ReactNode;
    description?: ReactNode;
    actions?: ReactNode;
    id?: string;
}

export function CardHeader({ title, eyebrow, description, actions, id }: CardHeaderProps) {
    return (
        <header className={styles.header}>
            <div className={styles.headingGroup}>
                {eyebrow && <span className={styles.eyebrow}>{eyebrow}</span>}
                <h2 className={styles.title} id={id}>
                    {title}
                </h2>
                {description && <p className={styles.description}>{description}</p>}
            </div>
            {actions && <div className={styles.actions}>{actions}</div>}
        </header>
    );
}

export function CardBody({
    children,
    flush = false,
    className,
}: {
    children: ReactNode;
    flush?: boolean;
    className?: string;
}) {
    return <div className={cx(styles.body, flush && styles.bodyFlush, className)}>{children}</div>;
}

export function CardFooter({ children }: { children: ReactNode }) {
    return <footer className={styles.footer}>{children}</footer>;
}
