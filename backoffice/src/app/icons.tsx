import type { ReactNode } from 'react';

/**
 * A hand-cut 16px icon set on a single grid. Seven nav glyphs plus three
 * utility marks is fewer than any icon package would install, and it keeps the
 * stroke weight identical to the rest of the interface.
 *
 * These are elements, not components: they are pure markup with no props, so a
 * shared frame function is all the abstraction they need.
 */
const frame = (children: ReactNode) => (
    <svg
        width="16"
        height="16"
        viewBox="0 0 16 16"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.4"
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden="true"
        focusable="false"
    >
        {children}
    </svg>
);

export const icons = {
    // Stacked cards: a backlog of beans.
    work: frame(
        <>
            <rect x="2.25" y="2.75" width="11.5" height="4" rx="1" />
            <rect x="2.25" y="9.25" width="11.5" height="4" rx="1" />
        </>,
    ),
    // A branch point.
    repositories: frame(
        <>
            <circle cx="4.5" cy="3.75" r="1.6" />
            <circle cx="4.5" cy="12.25" r="1.6" />
            <circle cx="11.5" cy="6.5" r="1.6" />
            <path d="M4.5 5.35v5.3M4.5 8.5h4.2a2.8 2.8 0 0 0 2.8-1.4" />
        </>,
    ),
    // A running process with a caret.
    agents: frame(
        <>
            <rect x="2" y="3" width="12" height="10" rx="1.5" />
            <path d="M5 6.5l2 1.5-2 1.5M8.75 10.5h2.5" />
        </>,
    ),
    // A folded record.
    memories: frame(
        <>
            <path d="M4 2.75h5.5L12.25 5.5v7.75H4z" />
            <path d="M9.25 2.9V5.6h2.8M6.25 8.5h3.5M6.25 10.75h3.5" />
        </>,
    ),
    // A meter dial: spend under a limit.
    cost: frame(
        <>
            <path d="M2.75 11.5a5.75 5.75 0 1 1 10.5 0" />
            <path d="M8 11.5L10.75 7" />
        </>,
    ),
    // A plug-in block.
    skills: frame(
        <>
            <rect x="3" y="3" width="10" height="10" rx="1.5" />
            <path d="M6.25 3V1.5M9.75 3V1.5M6.25 14.5V13M9.75 14.5V13" />
        </>,
    ),
    settings: frame(
        <>
            <circle cx="8" cy="8" r="2.1" />
            <path d="M8 1.75v1.6M8 12.65v1.6M14.25 8h-1.6M3.35 8h-1.6M12.4 3.6l-1.15 1.15M4.75 11.25L3.6 12.4M12.4 12.4l-1.15-1.15M4.75 4.75L3.6 3.6" />
        </>,
    ),
    lock: (
        <svg
            width="12"
            height="12"
            viewBox="0 0 12 12"
            fill="none"
            aria-hidden="true"
            focusable="false"
        >
            <rect
                x="2.75"
                y="5.25"
                width="6.5"
                height="5"
                rx="1"
                stroke="currentColor"
                strokeWidth="1.2"
            />
            <path
                d="M4.25 5.25V3.9a1.75 1.75 0 0 1 3.5 0v1.35"
                stroke="currentColor"
                strokeWidth="1.2"
            />
        </svg>
    ),
    sun: frame(
        <>
            <circle cx="8" cy="8" r="2.75" />
            <path d="M8 1.5v1.4M8 13.1v1.4M14.5 8h-1.4M2.9 8H1.5M12.6 3.4l-1 1M4.4 11.6l-1 1M12.6 12.6l-1-1M4.4 4.4l-1-1" />
        </>,
    ),
    moon: frame(<path d="M13 9.4A5.6 5.6 0 0 1 6.6 3a5.75 5.75 0 1 0 6.4 6.4z" />),
} as const;

export type IconName = keyof typeof icons;
