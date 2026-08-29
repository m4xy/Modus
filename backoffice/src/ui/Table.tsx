import type { ReactNode, TdHTMLAttributes, ThHTMLAttributes } from 'react';
import { cx } from './cx';
import styles from './Table.module.css';

export interface TableProps {
  /** Always supplied: a table without a caption is a table nobody can navigate. */
  caption: string;
  /** Hide the caption visually while keeping it for assistive technology. */
  hideCaption?: boolean;
  interactive?: boolean;
  children: ReactNode;
  className?: string;
}

export function Table({
  caption,
  hideCaption = true,
  interactive = false,
  children,
  className,
}: TableProps) {
  return (
    <div className={styles.scroller}>
      <table className={cx(styles.table, interactive && styles.interactive, className)}>
        <caption className={hideCaption ? 'visuallyHidden' : undefined}>{caption}</caption>
        {children}
      </table>
    </div>
  );
}

interface CellProps {
  numeric?: boolean;
  mono?: boolean;
  primary?: boolean;
}

export function Th({
  numeric,
  mono,
  className,
  ...rest
}: ThHTMLAttributes<HTMLTableCellElement> & CellProps) {
  return (
    <th
      scope="col"
      className={cx(numeric && styles.numeric, mono && styles.mono, className)}
      {...rest}
    />
  );
}

export function Td({
  numeric,
  mono,
  primary,
  className,
  ...rest
}: TdHTMLAttributes<HTMLTableCellElement> & CellProps) {
  return (
    <td
      className={cx(
        numeric && styles.numeric,
        mono && styles.mono,
        primary && styles.primaryCell,
        className,
      )}
      {...rest}
    />
  );
}
