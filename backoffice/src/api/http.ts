/**
 * The only module in the app permitted to touch the network.
 *
 * ESLint enforces this: `no-restricted-globals` bans `fetch` everywhere else
 * (see eslint.config.js), so a component that wants data has to come through
 * the typed client rather than inventing its own request.
 */

export const API_BASE = '/api';

export class ApiError extends Error {
    readonly status: number;
    readonly url: string;

    constructor(status: number, url: string, message: string) {
        super(message);
        this.name = 'ApiError';
        this.status = status;
        this.url = url;
    }
}

interface RequestOptions {
    signal?: AbortSignal;
    method?: 'GET' | 'POST' | 'PATCH' | 'DELETE';
    body?: unknown;
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
    const url = `${API_BASE}${path}`;
    const method = options.method ?? 'GET';

    const init: RequestInit = { method, headers: { Accept: 'application/json' } };
    if (options.signal) init.signal = options.signal;
    if (options.body !== undefined) {
        init.headers = { ...init.headers, 'Content-Type': 'application/json' };
        init.body = JSON.stringify(options.body);
    }

    const response = await fetch(url, init);

    if (!response.ok) {
        throw new ApiError(response.status, url, `${method} ${url} failed with ${response.status}`);
    }

    return (await response.json()) as T;
}

/**
 * Every tenant-scoped resource is addressed through this helper, so the
 * `/domains/{domainId}` root can never be accidentally dropped from a call.
 */
export function domainPath(domainId: string, resource: string): string {
    const suffix = resource.startsWith('/') ? resource : `/${resource}`;
    return `/domains/${encodeURIComponent(domainId)}${suffix}`;
}
