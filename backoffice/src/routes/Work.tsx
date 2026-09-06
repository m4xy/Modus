import { useMemo, useState } from 'react';
import { useDomain } from '../app/DomainContext';
import { useWorkItems } from '../api/queries';
import type { WorkItem, WorkStatus } from '../api/types';
import { PageHeader } from '../components/PageHeader';
import { Markdown } from '../components/Markdown';
import {
  Badge,
  Card,
  CardBody,
  Dialog,
  EmptyState,
  ErrorState,
  Input,
  Select,
  SkeletonList,
  Table,
  Td,
  Th,
} from '../ui';
import type { BadgeTone } from '../ui';
import { formatRelative, formatUsd } from '../lib/format';
import styles from './Work.module.css';

const statusTone: Record<WorkStatus, BadgeTone> = {
  backlog: 'neutral',
  ready: 'info',
  'in-progress': 'accent',
  'in-review': 'warn',
  blocked: 'danger',
  done: 'good',
};

const statusLabel: Record<WorkStatus, string> = {
  backlog: 'Backlog',
  ready: 'Ready',
  'in-progress': 'In progress',
  'in-review': 'In review',
  blocked: 'Blocked',
  done: 'Done',
};

export function Work() {
  const { domain } = useDomain();
  const query = useWorkItems(domain.id);
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState<'all' | WorkStatus>('all');
  const [open, setOpen] = useState<WorkItem | null>(null);

  const items = useMemo(() => {
    const all = query.data ?? [];
    const needle = search.trim().toLowerCase();
    return all.filter((item) => {
      const matchesStatus = status === 'all' || item.status === status;
      const matchesSearch =
        needle === '' ||
        item.title.toLowerCase().includes(needle) ||
        item.key.includes(needle) ||
        item.labels.some((label) => label.includes(needle));
      return matchesStatus && matchesSearch;
    });
  }, [query.data, search, status]);

  const header = (
    <PageHeader
      eyebrow={`${domain.id} · work`}
      title="Work"
      description="Every change in this domain traces to a numbered bean. These are the markdown files an agent reads before it starts, rendered for people."
    />
  );

  /*
    A failed read takes the whole screen, tiles and filters included. Rendering
    `0` and `$0.00` above an error would report two measurements nobody took,
    and a status filter over a list that was never fetched filters nothing
    (`bean:0140`).
  */
  if (query.isError) {
    return (
      <>
        {header}
        <ErrorState
          title="Work items could not be loaded"
          description="The request for this domain's backlog failed. Nothing here says the backlog is empty — it says it is unknown. Reload to try again."
        />
      </>
    );
  }

  /* `—` until the numbers are measured: a tile is a claim about the domain. */
  const loaded = query.data;
  const openCount = loaded ? loaded.filter((item) => item.status !== 'done').length : '—';
  const spend = loaded ? formatUsd(loaded.reduce((total, item) => total + item.spendUsd, 0)) : '—';
  const inReview = loaded ? loaded.filter((item) => item.status === 'in-review').length : '—';

  return (
    <>
      {header}

      <div className={styles.summaryRow}>
        <div className={styles.stat}>
          <span className={styles.statLabel}>Open items</span>
          <span className={styles.statValue}>{openCount}</span>
        </div>
        <div className={styles.stat}>
          <span className={styles.statLabel}>Spend attributed</span>
          <span className={styles.statValue}>{spend}</span>
        </div>
        <div className={styles.stat}>
          <span className={styles.statLabel}>In review</span>
          <span className={styles.statValue}>{inReview}</span>
        </div>
      </div>

      <div className={styles.toolbar}>
        <div className={styles.search}>
          <Input
            label="Search work items"
            placeholder="Title, bean number or label"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
        </div>
        <div className={styles.filter}>
          <Select
            label="Status"
            value={status}
            onChange={(event) => setStatus(event.target.value as 'all' | WorkStatus)}
            options={[
              { value: 'all', label: 'All statuses' },
              ...(Object.keys(statusLabel) as WorkStatus[]).map((value) => ({
                value,
                label: statusLabel[value],
              })),
            ]}
          />
        </div>
      </div>

      <Card>
        <CardBody flush>
          {query.isPending ? (
            <div style={{ padding: 'var(--space-5)' }}>
              <SkeletonList rows={5} label="Loading work items" />
            </div>
          ) : items.length === 0 ? (
            <EmptyState
              title={query.data?.length ? 'Nothing matches that filter' : 'No work items yet'}
              description={
                query.data?.length
                  ? 'Clear the search or pick a different status to see the rest of the backlog.'
                  : 'Beans are the unit of work in Modus. Create the first one and the harness has something to run against.'
              }
            />
          ) : (
            <Table caption={`Work items in ${domain.name}`} interactive>
              <thead>
                <tr>
                  <Th>Item</Th>
                  <Th>Status</Th>
                  <Th>Assignee</Th>
                  <Th numeric>Spend</Th>
                  <Th numeric>Updated</Th>
                </tr>
              </thead>
              <tbody>
                {items.map((item) => (
                  <tr key={item.id}>
                    <Td>
                      <button
                        type="button"
                        className={styles.rowButton}
                        onClick={() => setOpen(item)}
                        aria-haspopup="dialog"
                      >
                        <span className={styles.key}>
                          {item.kind.toUpperCase()} · {item.key}
                          {item.parentKey ? ` · under ${item.parentKey}` : ''}
                        </span>
                        <span className={styles.title}>{item.title}</span>
                        <span className={styles.labels}>
                          {item.labels.map((label) => (
                            <span className={styles.label} key={label}>
                              #{label}
                            </span>
                          ))}
                        </span>
                      </button>
                    </Td>
                    <Td>
                      <Badge tone={statusTone[item.status]} dot>
                        {statusLabel[item.status]}
                      </Badge>
                    </Td>
                    <Td mono>{item.assignee ?? 'unassigned'}</Td>
                    <Td numeric>{item.spendUsd === 0 ? '—' : formatUsd(item.spendUsd)}</Td>
                    <Td numeric mono>
                      {formatRelative(item.updatedAt)}
                    </Td>
                  </tr>
                ))}
              </tbody>
            </Table>
          )}
        </CardBody>
      </Card>

      <Dialog
        open={open !== null}
        onClose={() => setOpen(null)}
        title={open ? `${open.key} · ${open.title}` : ''}
      >
        {open && (
          <>
            <div className={styles.dialogMeta}>
              <Badge tone={statusTone[open.status]} dot>
                {statusLabel[open.status]}
              </Badge>
              <Badge tone="neutral">{open.kind}</Badge>
              {open.spendUsd > 0 && <Badge tone="spend">{formatUsd(open.spendUsd)}</Badge>}
            </div>
            <Markdown source={open.body} />
          </>
        )}
      </Dialog>
    </>
  );
}
