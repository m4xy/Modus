import { useId } from 'react';
import type { InputHTMLAttributes, ReactNode, TextareaHTMLAttributes } from 'react';
import { cx } from './cx';
import styles from './Field.module.css';

interface FieldShellProps {
    id: string;
    label: string;
    hint?: string | undefined;
    error?: string | undefined;
    hideLabel?: boolean | undefined;
    children: ReactNode;
}

function FieldShell({ id, label, hint, error, hideLabel, children }: FieldShellProps) {
    return (
        <div className={styles.field}>
            <label className={cx(styles.label, hideLabel && 'visuallyHidden')} htmlFor={id}>
                {label}
            </label>
            {children}
            {hint && !error && (
                <p className={styles.hint} id={`${id}-hint`}>
                    {hint}
                </p>
            )}
            {error && (
                <p className={styles.error} id={`${id}-error`}>
                    {error}
                </p>
            )}
        </div>
    );
}

function describedBy(id: string, hint?: string, error?: string): string | undefined {
    if (error) return `${id}-error`;
    if (hint) return `${id}-hint`;
    return undefined;
}

export interface InputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'id'> {
    label: string;
    hint?: string;
    error?: string;
    hideLabel?: boolean;
}

export function Input({ label, hint, error, hideLabel, className, ...rest }: InputProps) {
    const id = useId();
    return (
        <FieldShell id={id} label={label} hint={hint} error={error} hideLabel={hideLabel}>
            <input
                id={id}
                className={cx(styles.control, error && styles.invalid, className)}
                aria-invalid={error ? true : undefined}
                aria-describedby={describedBy(id, hint, error)}
                {...rest}
            />
        </FieldShell>
    );
}

export interface TextareaProps extends Omit<TextareaHTMLAttributes<HTMLTextAreaElement>, 'id'> {
    label: string;
    hint?: string;
    error?: string;
    hideLabel?: boolean;
}

export function Textarea({ label, hint, error, hideLabel, className, ...rest }: TextareaProps) {
    const id = useId();
    return (
        <FieldShell id={id} label={label} hint={hint} error={error} hideLabel={hideLabel}>
            <textarea
                id={id}
                className={cx(styles.control, styles.textarea, error && styles.invalid, className)}
                aria-invalid={error ? true : undefined}
                aria-describedby={describedBy(id, hint, error)}
                {...rest}
            />
        </FieldShell>
    );
}
