import { cx } from './cx';
import styles from './Skeleton.module.css';

export interface SkeletonProps {
  width?: string;
  height?: string;
  className?: string;
}

export function Skeleton({ width = '100%', height, className }: SkeletonProps) {
  return (
    <span
      className={cx(styles.skeleton, !height && styles.text, className)}
      style={height ? { width, height } : { width }}
    />
  );
}

/**
 * A loading placeholder that announces itself once, rather than a spinner that
 * says nothing. Widths taper so it reads as text, not as blocks.
 */
export function SkeletonList({ rows = 4, label = 'Loading' }: { rows?: number; label?: string }) {
  const widths = ['92%', '78%', '85%', '64%', '88%', '72%'];
  return (
    <div className={styles.stack} role="status" aria-live="polite" aria-busy="true">
      <span className="visuallyHidden">{label}</span>
      {Array.from({ length: rows }, (_, index) => (
        <Skeleton key={index} width={widths[index % widths.length] ?? '80%'} height="1rem" />
      ))}
    </div>
  );
}
