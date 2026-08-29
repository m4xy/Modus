import { cloneElement, useId, useState } from 'react';
import type { KeyboardEvent, ReactElement } from 'react';
import { cx } from './cx';
import styles from './Tooltip.module.css';

export interface TooltipProps {
  label: string;
  placement?: 'top' | 'bottom';
  children: ReactElement<{
    'aria-describedby'?: string | undefined;
    onKeyDown?: ((event: KeyboardEvent<HTMLElement>) => void) | undefined;
  }>;
}

/**
 * A description, not a replacement for a label: the trigger keeps its own
 * accessible name and the tooltip is wired with aria-describedby. It opens on
 * hover and on keyboard focus, and Escape dismisses it.
 */
export function Tooltip({ label, placement = 'top', children }: TooltipProps) {
  const id = useId();
  const [open, setOpen] = useState(false);

  /**
   * On the trigger, not on the wrapper span. Escape only ever reaches a keydown
   * handler when focus is inside, and focus lands on the trigger — so the wrapper
   * saw these events by bubbling, while carrying a key handler made a plain span
   * look interactive (jsx-a11y/no-static-element-interactions).
   */
  const onKeyDown = (event: KeyboardEvent<HTMLElement>) => {
    if (event.key === 'Escape' && open) {
      event.stopPropagation();
      setOpen(false);
    }
  };

  return (
    <span
      className={styles.wrapper}
      onMouseEnter={() => setOpen(true)}
      onMouseLeave={() => setOpen(false)}
      onFocusCapture={() => setOpen(true)}
      onBlurCapture={() => setOpen(false)}
    >
      {cloneElement(children, { 'aria-describedby': open ? id : undefined, onKeyDown })}
      {open && (
        <span
          role="tooltip"
          id={id}
          className={cx(styles.bubble, placement === 'bottom' && styles.below)}
        >
          {label}
        </span>
      )}
    </span>
  );
}
