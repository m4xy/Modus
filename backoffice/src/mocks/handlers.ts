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

/** A little latency so skeletons are exercised in development, none under test. */
const LATENCY_MS = import.meta.env['MODE'] === 'test' ? 0 : 180;

function scoped<T>(table: Record<string, T[]>, domainId: string | readonly string[] | undefined) {
  if (typeof domainId !== 'string') return undefined;
  return table[domainId];
}

export const handlers = [
  http.get(`${API_BASE}/session`, async () => {
    await delay(LATENCY_MS);
    return HttpResponse.json({ actor, domains, permissions });
  }),

  http.get(`${API_BASE}/domains/:domainId/work`, async ({ params }) => {
    await delay(LATENCY_MS);
    const items = scoped(workByDomain, params['domainId']);
    return items ? HttpResponse.json(items) : new HttpResponse(null, { status: 404 });
  }),

  http.get(`${API_BASE}/domains/:domainId/work/:key`, async ({ params }) => {
    await delay(LATENCY_MS);
    const items = scoped(workByDomain, params['domainId']);
    const item = items?.find((candidate) => candidate.key === params['key']);
    return item ? HttpResponse.json(item) : new HttpResponse(null, { status: 404 });
  }),

  http.get(`${API_BASE}/domains/:domainId/repositories`, async ({ params }) => {
    await delay(LATENCY_MS);
    const items = scoped(repositoriesByDomain, params['domainId']);
    return items ? HttpResponse.json(items) : new HttpResponse(null, { status: 404 });
  }),

  http.get(`${API_BASE}/domains/:domainId/agents/runs`, async ({ params }) => {
    await delay(LATENCY_MS);
    const items = scoped(agentRunsByDomain, params['domainId']);
    return items ? HttpResponse.json(items) : new HttpResponse(null, { status: 404 });
  }),

  http.get(`${API_BASE}/domains/:domainId/memories`, async ({ params }) => {
    await delay(LATENCY_MS);
    const items = scoped(memoriesByDomain, params['domainId']);
    return items ? HttpResponse.json(items) : new HttpResponse(null, { status: 404 });
  }),

  http.get(`${API_BASE}/domains/:domainId/skills`, async ({ params }) => {
    await delay(LATENCY_MS);
    const items = scoped(skillsByDomain, params['domainId']);
    return items ? HttpResponse.json(items) : new HttpResponse(null, { status: 404 });
  }),

  http.get(`${API_BASE}/domains/:domainId/cost/summary`, async ({ params }) => {
    await delay(LATENCY_MS);
    const domainId = params['domainId'];
    const summary = typeof domainId === 'string' ? costByDomain[domainId] : undefined;
    return summary ? HttpResponse.json(summary) : new HttpResponse(null, { status: 404 });
  }),
];
