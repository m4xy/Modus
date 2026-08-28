import { useEffect, useRef, useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router';
import { useDomain } from './DomainContext';
import { DomainSwitcher } from './DomainSwitcher';
import { navItems } from './navigation';
import { icons } from './icons';
import type { IconName } from './icons';
import { useTheme } from './ThemeProvider';
import { Button, Tooltip, useToast } from '../ui';
import { cx } from '../ui/cx';
import { formatUsd } from '../lib/format';
import styles from './AppShell.module.css';

function BudgetMeter() {
  const { domain } = useDomain();
  const ratio = domain.monthlyBudgetUsd ? domain.monthToDateSpendUsd / domain.monthlyBudgetUsd : 0;
  const over = ratio > 1;
  const percent = Math.round(ratio * 100);

  return (
    <div className={styles.meter}>
      <div className={styles.meterHead}>
        <span>Spend this month</span>
        <span>{percent}%</span>
      </div>
      <span className={styles.meterValue}>{formatUsd(domain.monthToDateSpendUsd)}</span>
      <div
        className={styles.meterTrack}
        role="meter"
        aria-valuenow={percent}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-label={`Spend against the monthly budget for ${domain.name}`}
      >
        <div
          className={cx(styles.meterFill, over && styles.meterFillOver)}
          style={{ width: `${Math.min(100, percent)}%` }}
        />
      </div>
      <span className={styles.meterFoot}>
        {over ? 'Over' : 'of'} {formatUsd(domain.monthlyBudgetUsd)} budget
      </span>
    </div>
  );
}

function ThemeToggle() {
  const { resolved, toggle } = useTheme();
  const next = resolved === 'dark' ? 'light' : 'dark';
  return (
    <Tooltip label={`Switch to ${next} theme`} placement="bottom">
      <Button
        variant="ghost"
        size="sm"
        iconOnly
        aria-label={`Switch to ${next} theme`}
        data-testid="theme-toggle"
        onClick={toggle}
      >
        {resolved === 'dark' ? icons.sun : icons.moon}
      </Button>
    </Tooltip>
  );
}

function ActorMenu() {
  const { actor, domain } = useDomain();
  const { preference, setPreference } = useTheme();
  const [open, setOpen] = useState(false);
  const anchorRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const onPointerDown = (event: PointerEvent) => {
      if (!anchorRef.current?.contains(event.target as Node)) setOpen(false);
    };
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpen(false);
    };
    document.addEventListener('pointerdown', onPointerDown);
    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.removeEventListener('pointerdown', onPointerDown);
      document.removeEventListener('keydown', onKeyDown);
    };
  }, [open]);

  const preferences: Array<{ id: 'light' | 'dark' | 'system'; label: string }> = [
    { id: 'light', label: 'Light' },
    { id: 'dark', label: 'Dark' },
    { id: 'system', label: 'Match system' },
  ];

  return (
    <div className={styles.popoverAnchor} ref={anchorRef}>
      <button
        type="button"
        className={styles.actorButton}
        aria-haspopup="menu"
        aria-expanded={open}
        data-testid="actor-menu"
        onClick={() => setOpen((value) => !value)}
      >
        <span className={styles.avatar} aria-hidden="true">
          {actor.initials}
        </span>
        <span className={styles.actorText}>
          <span className={styles.actorName}>{actor.name}</span>
          <span className={styles.actorRole}>
            {actor.role} · {domain.id}
          </span>
        </span>
      </button>

      {open && (
        <div className={styles.popover} role="menu" aria-label="Account">
          <p className={styles.popoverHeading}>Signed in as</p>
          <p className={styles.popoverMeta}>{actor.email}</p>
          <div className={styles.divider} />
          <p className={styles.popoverHeading}>Theme</p>
          {preferences.map((option) => (
            <button
              key={option.id}
              type="button"
              role="menuitemradio"
              aria-checked={preference === option.id}
              className={styles.popoverItem}
              onClick={() => setPreference(option.id)}
            >
              {option.label}
              {preference === option.id && <span aria-hidden="true">✓</span>}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

function PrimaryNav() {
  const { domain, can } = useDomain();
  const { notify } = useToast();

  return (
    <nav className={styles.railSection} aria-label="Primary">
      <p className={styles.railLabel} id="nav-heading">
        Sections
      </p>
      {navItems.map((item) => {
        const allowed = can(item.capability);

        if (!allowed && item.whenDenied === 'hide') return null;

        if (!allowed) {
          return (
            <button
              key={item.segment}
              type="button"
              aria-disabled="true"
              className={cx(styles.navItem, styles.navItemLocked)}
              data-testid={`nav-${item.segment}`}
              onClick={() =>
                notify({
                  tone: 'warning',
                  title: `${item.label} is not available in ${domain.name}`,
                  body: `Your permissions in this domain do not include ${item.capability}. Ask a domain owner to grant it.`,
                })
              }
            >
              <span className={styles.navIcon}>{icons[item.segment as IconName]}</span>
              {item.label}
              <span className={styles.navLock}>{icons.lock}</span>
            </button>
          );
        }

        return (
          <NavLink
            key={item.segment}
            to={`/domains/${domain.id}/${item.segment}`}
            className={({ isActive }) => cx(styles.navItem, isActive && styles.navItemActive)}
            data-testid={`nav-${item.segment}`}
          >
            <span className={styles.navIcon}>{icons[item.segment as IconName]}</span>
            {item.label}
          </NavLink>
        );
      })}
    </nav>
  );
}

export function AppShell() {
  const { domain, domains, can } = useDomain();
  const location = useLocation();
  const section = location.pathname.split('/')[3] ?? 'work';
  const current = navItems.find((item) => item.segment === section);

  return (
    <div className={styles.shell}>
      <a className="skipLink" href="#main">
        Skip to content
      </a>

      <aside className={styles.rail}>
        <div className={styles.brand}>
          <span className={styles.mark} aria-hidden="true">
            M
          </span>
          <span className={styles.wordmark}>Modus</span>
        </div>

        <DomainSwitcher current={domain} domains={domains} />

        <PrimaryNav />

        <div className={styles.railFooter}>
          {can('cost.read') && <BudgetMeter />}
          <ActorMenu />
        </div>
      </aside>

      <div className={styles.main}>
        <header className={styles.topbar}>
          <nav className={styles.crumbs} aria-label="Breadcrumb">
            <span>{domain.id}</span>
            <span aria-hidden="true">/</span>
            <span className={styles.crumbCurrent}>{current?.label ?? section}</span>
          </nav>
          <div className={styles.topbarActions}>
            <ThemeToggle />
          </div>
        </header>

        <main className={styles.content} id="main" tabIndex={-1}>
          <Outlet />
        </main>
      </div>
    </div>
  );
}
