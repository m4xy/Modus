import type { ButtonHTMLAttributes, ReactNode } from 'react';
import { cx } from './cx';
import styles from './Button.module.css';

export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger';
export type ButtonSize = 'sm' | 'md' | 'lg';

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
    variant?: ButtonVariant;
    size?: ButtonSize;
    /** Square button with no label — an accessible name is then required. */
    iconOnly?: boolean;
    children?: ReactNode;
}

export function Button({
    variant = 'secondary',
    size = 'md',
    iconOnly = false,
    className,
    type = 'button',
    ...rest
}: ButtonProps) {
    return (
        <button
            type={type}
            className={cx(
                styles.button,
                styles[variant],
                styles[size],
                iconOnly && styles.iconOnly,
                className,
            )}
            {...rest}
        />
    );
}
