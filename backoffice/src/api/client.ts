import { domainPath, request } from './http';
import type {
  AgentRun,
  CostSummary,
  DomainId,
  Memory,
  Repository,
  Session,
  Skill,
  WorkItem,
} from './types';

/**
 * Typed client. One function per endpoint, every tenant resource routed through
 * `domainPath` so the `/domains/{domainId}` root is structural rather than a
 * convention someone has to remember.
 */
export const api = {
  /** Not domain-scoped: this is what tells us which domains exist for the actor. */
  session: (signal?: AbortSignal) => request<Session>('/session', signal ? { signal } : {}),

  work: {
    list: (domainId: DomainId, signal?: AbortSignal) =>
      request<WorkItem[]>(domainPath(domainId, '/work'), signal ? { signal } : {}),
    get: (domainId: DomainId, key: string, signal?: AbortSignal) =>
      request<WorkItem>(domainPath(domainId, `/work/${key}`), signal ? { signal } : {}),
  },

  repositories: {
    list: (domainId: DomainId, signal?: AbortSignal) =>
      request<Repository[]>(domainPath(domainId, '/repositories'), signal ? { signal } : {}),
  },

  agents: {
    runs: (domainId: DomainId, signal?: AbortSignal) =>
      request<AgentRun[]>(domainPath(domainId, '/agents/runs'), signal ? { signal } : {}),
  },

  memories: {
    list: (domainId: DomainId, signal?: AbortSignal) =>
      request<Memory[]>(domainPath(domainId, '/memories'), signal ? { signal } : {}),
  },

  skills: {
    list: (domainId: DomainId, signal?: AbortSignal) =>
      request<Skill[]>(domainPath(domainId, '/skills'), signal ? { signal } : {}),
  },

  cost: {
    summary: (domainId: DomainId, signal?: AbortSignal) =>
      request<CostSummary>(domainPath(domainId, '/cost/summary'), signal ? { signal } : {}),
  },
} as const;
