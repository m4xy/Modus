import { useId } from 'react';
import type { SelectHTMLAttributes } from 'react';
import { cx } from './cx';
import styles from './Field.module.css';

export interface SelectOption {
    value: string;
    label: string;
    disabled?: boolean;
}

export interface SelectProps extends Omit<SelectHTMLAttributes<HTMLSelectElement>, 'id'> {
    label: string;
    options: SelectOption[];
    hint?: string;
    hideLabel?: boolean;
}

/**
 * A native select on purpose: it inherits platform keyboard behaviour, screen
 * reader semantics and mobile pickers for free. Custom listboxes earn their
 * complexity only when the options need rich content — none of ours do.
 */
export function Select({ label, options, hint, hideLabel, className, ...rest }: SelectProps) {
    const id = useId();
    return (
        <div className={styles.field}>
            <label className={cx(styles.label, hideLabel && 'visuallyHidden')} htmlFor={id}>
                {label}
            </label>
            <select
                id={id}
                className={cx(styles.control, styles.select, className)}
                aria-describedby={hint ? `${id}-hint` : undefined}
                {...rest}
            >
                {options.map((option) => (
                    <option
                        key={option.value}
                        value={option.value}
                        disabled={option.disabled ?? false}
                    >
                        {option.label}
                    </option>
                ))}
            </select>
            {hint && (
                <p className={styles.hint} id={`${id}-hint`}>
                    {hint}
                </p>
            )}
        </div>
    );
}
