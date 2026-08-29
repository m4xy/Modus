const usd = new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
});

const usdCompact = new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    notation: 'compact',
    maximumFractionDigits: 1,
});

const usdPrecise = new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 4,
    maximumFractionDigits: 4,
});

export function formatUsd(value: number): string {
    return usd.format(value);
}

/** For axis ticks and dense tiles where the exact cent is noise. */
export function formatUsdCompact(value: number): string {
    return value >= 1000 ? usdCompact.format(value) : usd.format(value);
}

/** Live counters move in fractions of a cent — round early and you show $0.00. */
export function formatUsdPrecise(value: number): string {
    return value < 1 ? usdPrecise.format(value) : usd.format(value);
}

const tokens = new Intl.NumberFormat('en-US', { notation: 'compact', maximumFractionDigits: 1 });

export function formatTokens(value: number): string {
    return tokens.format(value);
}

const integers = new Intl.NumberFormat('en-US');

export function formatCount(value: number): string {
    return integers.format(value);
}

export function formatPercent(value: number): string {
    return `${Math.round(value * 100)}%`;
}

const dayMonth = new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'short' });

export function formatDay(iso: string): string {
    return dayMonth.format(new Date(iso));
}

const dateTime = new Intl.DateTimeFormat('en-GB', {
    day: 'numeric',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
    timeZone: 'UTC',
});

export function formatDateTime(iso: string): string {
    return `${dateTime.format(new Date(iso))} UTC`;
}

export function formatDuration(ms: number): string {
    if (ms < 1000) return `${ms}ms`;
    const seconds = Math.round(ms / 1000);
    if (seconds < 60) return `${seconds}s`;
    const minutes = Math.floor(seconds / 60);
    const remainder = seconds % 60;
    return remainder === 0 ? `${minutes}m` : `${minutes}m ${remainder}s`;
}

/** "3 days ago" reads faster than a timestamp when scanning a list. */
export function formatRelative(iso: string, now: Date = new Date('2026-08-28T15:00:00Z')): string {
    const diffMs = now.getTime() - new Date(iso).getTime();
    const minutes = Math.round(diffMs / 60_000);
    if (minutes < 1) return 'just now';
    if (minutes < 60) return `${minutes}m ago`;
    const hours = Math.round(minutes / 60);
    if (hours < 24) return `${hours}h ago`;
    const days = Math.round(hours / 24);
    return days === 1 ? 'yesterday' : `${days}d ago`;
}
