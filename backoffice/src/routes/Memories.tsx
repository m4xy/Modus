import { useDomain } from '../app/DomainContext';
import { useMemories } from '../api/queries';
import { PageHeader } from '../components/PageHeader';
import { Badge, Card, CardBody, CardHeader, EmptyState, SkeletonList } from '../ui';
import { formatRelative, formatTokens } from '../lib/format';

export function Memories() {
  const { domain } = useDomain();
  const query = useMemories(domain.id);
  const memories = query.data ?? [];
  const totalTokens = memories.reduce((sum, memory) => sum + memory.tokens, 0);

  return (
    <>
      <PageHeader
        eyebrow={`${domain.id} · memories`}
        title="Memories"
        description="What this domain carries into every run. Memories are loaded before the prompt, so they are a standing cost as well as a standing instruction."
        actions={
          memories.length > 0 ? (
            <Badge tone="spend">{formatTokens(totalTokens)} tokens per run</Badge>
          ) : undefined
        }
      />

      {query.isPending ? (
        <SkeletonList rows={4} label="Loading memories" />
      ) : memories.length === 0 ? (
        <Card>
          <CardBody>
            <EmptyState
              title="Nothing remembered yet"
              description="Memories are the rules an agent should not have to be told twice. The first one usually comes out of a code review."
            />
          </CardBody>
        </Card>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-4)' }}>
          {memories.map((memory) => (
            <Card key={memory.id}>
              <CardHeader
                eyebrow={`${memory.scope} · ${formatTokens(memory.tokens)} tokens`}
                title={memory.title}
                actions={<Badge tone="neutral">{formatRelative(memory.updatedAt)}</Badge>}
              />
              <CardBody>
                <p style={{ color: 'var(--ink-2)', fontSize: 'var(--text-sm)', maxWidth: '68ch' }}>
                  {memory.excerpt}
                </p>
              </CardBody>
            </Card>
          ))}
        </div>
      )}
    </>
  );
}
