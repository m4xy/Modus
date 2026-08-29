import { useId, useRef } from 'react';
import type { KeyboardEvent, ReactNode } from 'react';
import { cx } from './cx';
import styles from './Tabs.module.css';

export interface TabDefinition {
    id: string;
    label: string;
    /** Optional trailing count, e.g. how many rows the panel holds. */
    count?: number;
    panel: ReactNode;
}

export interface TabsProps {
    tabs: TabDefinition[];
    value: string;
    onChange: (id: string) => void;
    label: string;
}

/**
 * WAI-ARIA tabs with a roving tabindex: only the selected tab is in the tab
 * order, and arrow keys move between tabs, which is what a screen reader user
 * expects here rather than seven stops through the tab strip.
 */
export function Tabs({ tabs, value, onChange, label }: TabsProps) {
    const baseId = useId();
    const listRef = useRef<HTMLDivElement>(null);

    const move = (delta: number) => {
        const index = tabs.findIndex((tab) => tab.id === value);
        if (index === -1) return;
        const next = tabs[(index + delta + tabs.length) % tabs.length];
        if (!next) return;
        onChange(next.id);
        listRef.current
            ?.querySelector<HTMLButtonElement>(`#${CSS.escape(`${baseId}-${next.id}-tab`)}`)
            ?.focus();
    };

    const onKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
        switch (event.key) {
            case 'ArrowRight':
                event.preventDefault();
                move(1);
                break;
            case 'ArrowLeft':
                event.preventDefault();
                move(-1);
                break;
            case 'Home':
                event.preventDefault();
                if (tabs[0]) onChange(tabs[0].id);
                break;
            case 'End':
                event.preventDefault();
                if (tabs.length > 0) onChange(tabs[tabs.length - 1]!.id);
                break;
            default:
                break;
        }
    };

    const active = tabs.find((tab) => tab.id === value) ?? tabs[0];

    return (
        <div>
            <div
                className={styles.tablist}
                role="tablist"
                aria-label={label}
                ref={listRef}
                onKeyDown={onKeyDown}
            >
                {tabs.map((tab) => {
                    const selected = tab.id === value;
                    return (
                        <button
                            key={tab.id}
                            type="button"
                            role="tab"
                            id={`${baseId}-${tab.id}-tab`}
                            aria-selected={selected}
                            aria-controls={`${baseId}-${tab.id}-panel`}
                            tabIndex={selected ? 0 : -1}
                            className={cx(styles.tab, selected && styles.selected)}
                            onClick={() => onChange(tab.id)}
                        >
                            {tab.label}
                            {tab.count !== undefined && (
                                <span className={styles.count}>{tab.count}</span>
                            )}
                        </button>
                    );
                })}
            </div>
            {active && (
                <div
                    className={styles.panel}
                    role="tabpanel"
                    id={`${baseId}-${active.id}-panel`}
                    aria-labelledby={`${baseId}-${active.id}-tab`}
                    tabIndex={0}
                >
                    {active.panel}
                </div>
            )}
        </div>
    );
}
