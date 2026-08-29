import { useDomain } from '../app/DomainContext';
import { useSkills } from '../api/queries';
import { PageHeader } from '../components/PageHeader';
import { Badge, Card, CardBody, EmptyState, SkeletonList, Table, Td, Th } from '../ui';
import { formatCount } from '../lib/format';

export function Skills() {
  const { domain } = useDomain();
  const query = useSkills(domain.id);

  return (
    <>
      <PageHeader
        eyebrow={`${domain.id} · skills`}
        title="Skills"
        description="Modules installed in this domain. An agent can call any skill that is enabled here, and nothing else."
      />

      <Card>
        <CardBody flush>
          {query.isPending ? (
            <div style={{ padding: 'var(--space-5)' }}>
              <SkeletonList rows={3} label="Loading skills" />
            </div>
          ) : (query.data ?? []).length === 0 ? (
            <EmptyState
              title="No skills installed"
              description="Install a skill and every agent in this domain gains that capability on its next run."
            />
          ) : (
            <Table caption={`Skills installed in ${domain.name}`}>
              <thead>
                <tr>
                  <Th>Skill</Th>
                  <Th>Version</Th>
                  <Th>State</Th>
                  <Th numeric>Invocations (30d)</Th>
                </tr>
              </thead>
              <tbody>
                {(query.data ?? []).map((skill) => (
                  <tr key={skill.id}>
                    <Td primary>
                      {skill.name}
                      <div
                        style={{
                          fontSize: 'var(--text-xs)',
                          color: 'var(--ink-2)',
                        }}
                      >
                        {skill.summary}
                      </div>
                    </Td>
                    <Td mono>{skill.installedVersion}</Td>
                    <Td>
                      <Badge tone={skill.enabled ? 'good' : 'neutral'} dot>
                        {skill.enabled ? 'enabled' : 'disabled'}
                      </Badge>
                    </Td>
                    <Td numeric mono>
                      {formatCount(skill.invocations30d)}
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
