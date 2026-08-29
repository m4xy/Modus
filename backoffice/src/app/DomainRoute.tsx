import { Navigate, useParams } from 'react-router';
import { useSession } from '../api/queries';
import { DomainProvider } from './DomainContext';
import { AppShell } from './AppShell';
import { EmptyState, SkeletonList } from '../ui';
import styles from './DomainRoute.module.css';

/**
 * Resolves `/domains/:domainId` into a tenant before anything else renders.
 * An unknown or unpermitted domain never reaches a screen — the actor is sent
 * to the first domain they can actually see.
 */
export function DomainRoute() {
    const params = useParams();
    const domainId = params['domainId'];
    const session = useSession();

    if (session.isPending) {
        return (
            <div className={styles.boot} role="status" aria-live="polite">
                <p className={styles.bootLabel}>Loading domains</p>
                <SkeletonList rows={3} label="Loading domains" />
            </div>
        );
    }

    if (session.isError || !session.data) {
        return (
            <div className={styles.boot}>
                <EmptyState
                    title="Cannot reach the Modus API"
                    description="The session request failed. Reload the page; if it keeps failing, the API is down."
                />
            </div>
        );
    }

    const { actor, domains, permissions } = session.data;
    const domain = domains.find((candidate) => candidate.id === domainId);

    if (!domain) {
        const fallback = domains[0];
        if (!fallback) {
            return (
                <div className={styles.boot}>
                    <EmptyState
                        title="No domains yet"
                        description="This account has no domain grants. A domain owner needs to invite you before there is anything to orchestrate."
                    />
                </div>
            );
        }
        return <Navigate to={`/domains/${fallback.id}/work`} replace />;
    }

    return (
        <DomainProvider domain={domain} domains={domains} actor={actor} permissions={permissions}>
            <AppShell />
        </DomainProvider>
    );
}
