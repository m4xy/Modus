import { useDomain } from '../app/DomainContext';
import { useRepositories } from '../api/queries';
import { PageHeader } from '../components/PageHeader';
import { Badge, Card, CardBody, EmptyState, SkeletonList, Table, Td, Th, Tooltip } from '../ui';
import type { BadgeTone } from '../ui';
import { formatRelative } from '../lib/format';

const tone: Record<string, BadgeTone> = {
  connected: 'good',
  syncing: 'info',
  error: 'danger',
};

export function Repositories() {
  const { domain } = useDomain();
  const query = useRepositories(domain.id);

  return (
    <>
      <PageHeader
        eyebrow={`${domain.id} · repositories`}
        title="Repositories"
        description="Repositories this domain can act on. An agent clones from here and pushes branches back."
      />

      <Card>
        <CardBody flush>
          {query.isPending ? (
            <div style={{ padding: 'var(--space-5)' }}>
              <SkeletonList rows={3} label="Loading repositories" />
            </div>
          ) : (query.data ?? []).length === 0 ? (
            <EmptyState
              title="No repositories connected"
              description="Connect a repository and this domain gains somewhere to put the work it produces."
            />
          ) : (
            <Table caption={`Repositories connected to ${domain.name}`}>
              <thead>
                <tr>
                  <Th>Repository</Th>
                  <Th>Default branch</Th>
                  <Th>Status</Th>
                  <Th numeric>Open items</Th>
                  <Th numeric>Last synced</Th>
                </tr>
              </thead>
              <tbody>
                {(query.data ?? []).map((repo) => (
                  <tr key={repo.id}>
                    <Td primary>
                      {repo.name}
                      <div
                        style={{
                          fontFamily: 'var(--font-mono)',
                          fontSize: 'var(--text-2xs)',
                          color: 'var(--ink-3)',
                        }}
                      >
                        {repo.remote}
                      </div>
                    </Td>
                    <Td mono>{repo.defaultBranch}</Td>
                    <Td>
                      {repo.status === 'error' ? (
                        <Tooltip label="Last sync failed: authentication rejected">
                          <span>
                            <Badge tone={tone[repo.status] ?? 'neutral'} dot>
                              {repo.status}
                            </Badge>
                          </span>
                        </Tooltip>
                      ) : (
                        <Badge tone={tone[repo.status] ?? 'neutral'} dot>
                          {repo.status}
                        </Badge>
                      )}
                    </Td>
                    <Td numeric mono>
                      {repo.openWorkItems}
                    </Td>
                    <Td numeric mono>
                      {formatRelative(repo.lastSyncedAt)}
                    </Td>
                  </tr>
                ))}
              </tbody>
            </Table>
          )}
        </CardBody>
      </Card>
    </>
  );
}
