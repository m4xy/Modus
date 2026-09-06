import type { ReactNode } from 'react';
import { EmptyState } from './EmptyState';

export interface ErrorStateProps {
  title: string;
  description: string;
  action?: ReactNode;
}

/**
 * "The request failed" — which is never the same statement as "there is nothing
 * here", and was rendered as if it were on five screens (`bean:0140`). A route
 * that branches only on `isPending` falls through to its empty state when the
 * query rejects, so a 500 and an empty collection produce the same pixels: the
 * operator is told the backlog is empty when what happened is that nothing was
 * read.
 *
 * It carries `role="alert"` because a failed read is an announcement rather than
 * a decoration, and a `data-testid` because a screen that cannot be observed
 * failing has not been fixed (`doc:00-constitution#observed-failing`).
 */
export function ErrorState({ title, description, action }: ErrorStateProps) {
  return (
    <div role="alert" data-testid="error-state">
      <EmptyState title={title} description={description} mark="!" action={action} />
    </div>
  );
}
