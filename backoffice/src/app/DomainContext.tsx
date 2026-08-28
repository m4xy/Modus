import { createContext, useContext, useMemo } from 'react';
import type { ReactNode } from 'react';
import type { Actor, Capability, Domain, Permissions } from '../api/types';

interface DomainContextValue {
  domain: Domain;
  domains: Domain[];
  actor: Actor;
  capabilities: Capability[];
  can: (capability: Capability) => boolean;
}

const DomainContext = createContext<DomainContextValue | null>(null);

/**
 * Everything below the shell reads its tenant from here. There is deliberately
 * no "current domain" singleton: the domain comes from the URL, so a link is
 * always enough to put a colleague in exactly the same place.
 */
export function useDomain(): DomainContextValue {
  const context = useContext(DomainContext);
  if (!context) throw new Error('useDomain must be used inside <DomainProvider>.');
  return context;
}

export function DomainProvider({
  domain,
  domains,
  actor,
  permissions,
  children,
}: {
  domain: Domain;
  domains: Domain[];
  actor: Actor;
  permissions: Permissions[];
  children: ReactNode;
}) {
  const value = useMemo<DomainContextValue>(() => {
    const grant = permissions.find((entry) => entry.domainId === domain.id);
    const capabilities = grant?.capabilities ?? [];
    return {
      domain,
      domains,
      actor,
      capabilities,
      can: (capability: Capability) => capabilities.includes(capability),
    };
  }, [domain, domains, actor, permissions]);

  return <DomainContext.Provider value={value}>{children}</DomainContext.Provider>;
}
