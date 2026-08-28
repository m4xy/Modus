import { Navigate } from 'react-router';
import { useSession } from '../api/queries';
import { EmptyState, SkeletonList } from '../ui';
import styles from './DomainRoute.module.css';

/**
 * There is no "home" above a domain — the product starts at a tenant. This
 * sends the actor to the first domain they hold a grant for.
 */
export function RootRedirect() {
  const session = useSession();

  if (session.isPending) {
    return (
      <div className={styles.boot} role="status" aria-live="polite">
        <p className={styles.bootLabel}>Opening Modus</p>
        <SkeletonList rows={2} label="Opening Modus" />
      </div>
    );
  }

  const first = session.data?.domains[0];
  if (!first) {
    return (
      <div className={styles.boot}>
        <EmptyState
          title="No domains available"
          description="Your account holds no domain grants yet. Ask a domain owner to invite you."
        />
      </div>
    );
  }

  return <Navigate to={`/domains/${first.id}/work`} replace />;
}
