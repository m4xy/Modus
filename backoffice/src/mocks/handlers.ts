import { HttpResponse, http, delay } from 'msw';
import { API_BASE } from '../api/http';
import {
  actor,
  agentRunsByDomain,
  costByDomain,
  domains,
  memoriesByDomain,
  permissions,
  repositoriesByDomain,
  skillsByDomain,
  workByDomain,
} from './data';

/**
 * Fault injection, in the shape the agent console already uses for its stream
 * (`faultFromLocation`, `src/agent/mockTransport.ts`): `?fail=<resource>` on the
 * page URL makes that resource answer `500`, and `?fail=all` makes every one of
 * them do it. Comma-separate to fail several.
 *
 * It lives here because there is no other way to reach the error branches from a
 * test. MSW's service worker answers before the request reaches the network, so
 * Playwright's `page.route` never sees these calls and cannot fulfil them with a
 * non-2xx. Five screens rendered a rejected query as an empty collection
 * precisely because nothing could make one reject (`bean:0140`).
 *
 * **This module ships.** The first version of this comment said the opposite —
 * that `VITE_MOCK_API=false` drops it from the bundle — which was a claim about
 * the build that had never been checked against the build, committed to the one
 * change whose whole subject is mechanisms reporting success without checking.
 * Built both ways, the chunk is emitted either way:
 *
 * ```
 * VITE_MOCK_API=false npx vite build  ->  browser-C2ngF1C2.js  434367 bytes
 * npx vite build                      ->  browser-Ck4cxOVV.js  434345 bytes
 * ```
 *
 * and the `VITE_MOCK_API=false` output still contains `URLSearchParams`, `fail`
 * and four `status:500` sites. The cause is bracket access:
 * `import.meta.env['VITE_MOCK_API']` in `src/main.tsx` compiles to a runtime
 * comparison rather than folding to a constant, so Rollup cannot eliminate the
 * dynamic `import()`. Nothing in this repository sets the flag to `false` in any
 * case — no `.env` file, no deploy step, no CI variable — so under every build
 * actually run here, MSW installs and `?fail=all` is live on the shipped bundle.
 * `bean:0146` owns the `VITE_MOCK_API=false` path; `bean:0191` owns making the
 * flag statically foldable so this module can actually be dropped.
 *
 * Handlers run in the page rather than in the worker, so `window.location` is
 * the location of the document that issued the request.
 */
function failing(resource: string): boolean {
  const raw = new URLSearchParams(window.location.search).get('fail');
  if (raw === null) return false;
  return raw === 'all' || raw.split(',').includes(resource);
}

const serverError = () => new HttpResponse(null, { status: 500 });

/** A little latency so skeletons are exercised in development, none under test. */
const LATENCY_MS = import.meta.env['MODE'] === 'test' ? 0 : 180;

function scoped<T>(table: Record<string, T[]>, domainId: string | readonly string[] | undefined) {
  if (typeof domainId !== 'string') return undefined;
  return table[domainId];
}

export const handlers = [
  http.get(`${API_BASE}/session`, async () => {
    await delay(LATENCY_MS);
    if (failing('session')) return serverError();
    return HttpResponse.json({ actor, domains, permissions });
  }),

  http.get(`${API_BASE}/domains/:domainId/work`, async ({ params }) => {
    await delay(LATENCY_MS);
    if (failing('work')) return serverError();
    const items = scoped(workByDomain, params['domainId']);
    return items ? HttpResponse.json(items) : new HttpResponse(null, { status: 404 });
  }),

  http.get(`${API_BASE}/domains/:domainId/work/:key`, async ({ params }) => {
    await delay(LATENCY_MS);
    if (failing('work')) return serverError();
    const items = scoped(workByDomain, params['domainId']);
    const item = items?.find((candidate) => candidate.key === params['key']);
    return item ? HttpResponse.json(item) : new HttpResponse(null, { status: 404 });
  }),

  http.get(`${API_BASE}/domains/:domainId/repositories`, async ({ params }) => {
    await delay(LATENCY_MS);
    if (failing('repositories')) return serverError();
    const items = scoped(repositoriesByDomain, params['domainId']);
    return items ? HttpResponse.json(items) : new HttpResponse(null, { status: 404 });
  }),

  http.get(`${API_BASE}/domains/:domainId/agents/runs`, async ({ params }) => {
    await delay(LATENCY_MS);
    if (failing('runs')) return serverError();
    const items = scoped(agentRunsByDomain, params['domainId']);
    return items ? HttpResponse.json(items) : new HttpResponse(null, { status: 404 });
  }),

  http.get(`${API_BASE}/domains/:domainId/memories`, async ({ params }) => {
    await delay(LATENCY_MS);
    if (failing('memories')) return serverError();
    const items = scoped(memoriesByDomain, params['domainId']);
    return items ? HttpResponse.json(items) : new HttpResponse(null, { status: 404 });
  }),

  http.get(`${API_BASE}/domains/:domainId/skills`, async ({ params }) => {
    await delay(LATENCY_MS);
    if (failing('skills')) return serverError();
    const items = scoped(skillsByDomain, params['domainId']);
    return items ? HttpResponse.json(items) : new HttpResponse(null, { status: 404 });
  }),

  http.get(`${API_BASE}/domains/:domainId/cost/summary`, async ({ params }) => {
    await delay(LATENCY_MS);
    if (failing('cost')) return serverError();
    const domainId = params['domainId'];
    const summary = typeof domainId === 'string' ? costByDomain[domainId] : undefined;
    return summary ? HttpResponse.json(summary) : new HttpResponse(null, { status: 404 });
  }),
];
