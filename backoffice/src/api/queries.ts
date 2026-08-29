import { useQuery } from '@tanstack/react-query';
import { api } from './client';
import type { DomainId } from './types';

/**
 * Query keys always lead with the domain so that switching tenant swaps the
 * whole cache namespace instead of leaking one domain's data into another's view.
 */
export const queryKeys = {
  session: ['session'] as const,
  domain: (domainId: DomainId) => ['domain', domainId] as const,
  work: (domainId: DomainId) => ['domain', domainId, 'work'] as const,
  repositories: (domainId: DomainId) => ['domain', domainId, 'repositories'] as const,
  agentRuns: (domainId: DomainId) => ['domain', domainId, 'agents', 'runs'] as const,
  memories: (domainId: DomainId) => ['domain', domainId, 'memories'] as const,
  skills: (domainId: DomainId) => ['domain', domainId, 'skills'] as const,
  cost: (domainId: DomainId) => ['domain', domainId, 'cost'] as const,
};

export function useSession() {
  return useQuery({
    queryKey: queryKeys.session,
    queryFn: ({ signal }) => api.session(signal),
    staleTime: 5 * 60 * 1000,
  });
}

export function useWorkItems(domainId: DomainId) {
  return useQuery({
    queryKey: queryKeys.work(domainId),
    queryFn: ({ signal }) => api.work.list(domainId, signal),
  });
}

export function useRepositories(domainId: DomainId) {
  return useQuery({
    queryKey: queryKeys.repositories(domainId),
    queryFn: ({ signal }) => api.repositories.list(domainId, signal),
  });
}

export function useAgentRuns(domainId: DomainId) {
  return useQuery({
    queryKey: queryKeys.agentRuns(domainId),
    queryFn: ({ signal }) => api.agents.runs(domainId, signal),
  });
}

export function useMemories(domainId: DomainId) {
  return useQuery({
    queryKey: queryKeys.memories(domainId),
    queryFn: ({ signal }) => api.memories.list(domainId, signal),
  });
}

export function useSkills(domainId: DomainId) {
  return useQuery({
    queryKey: queryKeys.skills(domainId),
    queryFn: ({ signal }) => api.skills.list(domainId, signal),
  });
}

export function useCostSummary(domainId: DomainId) {
  return useQuery({
    queryKey: queryKeys.cost(domainId),
    queryFn: ({ signal }) => api.cost.summary(domainId, signal),
  });
}
