import type { ReactNode } from 'react';
import { useDomain } from '../app/DomainContext';
import type { Capability } from '../api/types';
import { EmptyState } from '../ui';

/**
 * Route-level counterpart to the permission-aware navigation. Hiding a link is
 * a courtesy; refusing to render the screen is the actual boundary — a
 * hand-typed URL has to hit the same wall.
 */
export function RequireCapability({
  capability,
  children,
}: {
  capability: Capability;
  children: ReactNode;
}) {
  const { can, domain } = useDomain();

  if (!can(capability)) {
    return (
      <EmptyState
        title="You do not have access to this section"
        description={`Viewing it in ${domain.name} needs the ${capability} permission. A domain owner can grant it from Settings → Actors.`}
      />
    );
  }

  return <>{children}</>;
}
