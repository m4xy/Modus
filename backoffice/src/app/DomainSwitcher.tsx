import { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router';
import type { Domain } from '../api/types';
import { Badge } from '../ui';
import { formatUsd } from '../lib/format';
import { cx } from '../ui/cx';
import styles from './DomainSwitcher.module.css';

const environmentTone = {
  production: 'good',
  staging: 'info',
  sandbox: 'neutral',
} as const;

/**
 * The most important control in the app: every screen below it is scoped to
 * `/domains/{domainId}`, so switching domain is a navigation, not a filter.
 * The current section is preserved across the switch where it exists.
 */
export function DomainSwitcher({ current, domains }: { current: Domain; domains: Domain[] }) {
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const menuRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    if (!open) return;
    const onPointerDown = (event: PointerEvent) => {
      if (!rootRef.current?.contains(event.target as Node)) setOpen(false);
    };
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setOpen(false);
        triggerRef.current?.focus();
      }
    };
    document.addEventListener('pointerdown', onPointerDown);
    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.removeEventListener('pointerdown', onPointerDown);
      document.removeEventListener('keydown', onKeyDown);
    };
  }, [open]);

  useEffect(() => {
    if (open) menuRef.current?.querySelector<HTMLButtonElement>('button')?.focus();
  }, [open]);

  const section = location.pathname.split('/')[3] ?? 'work';

  const select = (domain: Domain) => {
    setOpen(false);
    void navigate(`/domains/${domain.id}/${section}`);
    triggerRef.current?.focus();
  };

  const moveFocus = (delta: number) => {
    const buttons = Array.from(menuRef.current?.querySelectorAll('button') ?? []);
    const index = buttons.indexOf(document.activeElement as HTMLButtonElement);
    const next = buttons[(index + delta + buttons.length) % buttons.length];
    next?.focus();
  };

  return (
    <div className={styles.root} ref={rootRef}>
      <button
        type="button"
        ref={triggerRef}
        className={cx(styles.trigger, open && styles.triggerOpen)}
        aria-haspopup="menu"
        aria-expanded={open}
        data-testid="domain-switcher"
        onClick={() => setOpen((value) => !value)}
        onKeyDown={(event) => {
          if (event.key === 'ArrowDown') {
            event.preventDefault();
            setOpen(true);
          }
        }}
      >
        <span className={styles.glyph} aria-hidden="true">
          {current.name.slice(0, 2).toUpperCase()}
        </span>
        <span className={styles.labels}>
          <span className={styles.eyebrow}>Domain</span>
          <span className={styles.name} data-testid="current-domain">
            {current.name}
          </span>
        </span>
        <svg
          className={cx(styles.chevron, open && styles.chevronOpen)}
          width="12"
          height="12"
          viewBox="0 0 12 12"
          aria-hidden="true"
        >
          <path
            d="M2.5 4.5L6 8l3.5-3.5"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      </button>

      {open && (
        <div
          className={styles.menu}
          role="menu"
          aria-label="Switch domain"
          ref={menuRef}
          data-testid="domain-menu"
          onKeyDown={(event) => {
            if (event.key === 'ArrowDown') {
              event.preventDefault();
              moveFocus(1);
            }
            if (event.key === 'ArrowUp') {
              event.preventDefault();
              moveFocus(-1);
            }
          }}
        >
          <p className={styles.menuHeading}>Domains you can access</p>
          {domains.map((domain) => {
            const isCurrent = domain.id === current.id;
            return (
              <button
                key={domain.id}
                type="button"
                role="menuitem"
                className={cx(styles.option, isCurrent && styles.optionCurrent)}
                onClick={() => select(domain)}
              >
                <span className={styles.optionName}>{domain.name}</span>
                <Badge tone={environmentTone[domain.environment]} dot>
                  {domain.environment}
                </Badge>
                <span className={styles.optionMeta}>
                  {formatUsd(domain.monthToDateSpendUsd)} of {formatUsd(domain.monthlyBudgetUsd)}{' '}
                  this month
                  {isCurrent ? ' · current' : ''}
                </span>
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}
