import { useState } from 'react';
import { useDomain } from '../app/DomainContext';
import { PageHeader } from '../components/PageHeader';
import { Badge, Button, Card, CardBody, CardHeader, Input, Select, useToast } from '../ui';

export function Settings() {
  const { domain, can, capabilities } = useDomain();
  const { notify } = useToast();
  const writable = can('settings.write');
  const [budget, setBudget] = useState(String(domain.monthlyBudgetUsd));

  return (
    <>
      <PageHeader
        eyebrow={`${domain.id} · settings`}
        title="Settings"
        description="Configuration for this domain. Changes apply to every actor and every run in it."
        actions={
          writable ? undefined : (
            <Badge tone="warn">Read only — you cannot change settings here</Badge>
          )
        }
      />

      <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-5)' }}>
        <Card>
          <CardHeader
            eyebrow="Identity"
            title="Domain"
            description="The tenant root every URL in this app hangs off."
          />
          <CardBody>
            <div style={{ display: 'grid', gap: 'var(--space-4)', maxWidth: '32rem' }}>
              <Input label="Domain id" value={domain.id} readOnly disabled />
              <Input label="Display name" defaultValue={domain.name} disabled={!writable} />
              <Select
                label="Environment"
                defaultValue={domain.environment}
                disabled={!writable}
                options={[
                  { value: 'production', label: 'Production' },
                  { value: 'staging', label: 'Staging' },
                  { value: 'sandbox', label: 'Sandbox' },
                ]}
              />
            </div>
          </CardBody>
        </Card>

        <Card>
          <CardHeader
            eyebrow="Spend"
            title="Monthly budget"
            description="Runs are refused once the domain crosses its budget."
          />
          <CardBody>
            <div style={{ display: 'grid', gap: 'var(--space-4)', maxWidth: '32rem' }}>
              <Input
                label="Budget (USD)"
                inputMode="decimal"
                value={budget}
                disabled={!writable}
                hint="Applies to model spend only. Infrastructure is billed separately."
                onChange={(event) => setBudget(event.target.value)}
              />
              <div>
                <Button
                  variant="primary"
                  disabled={!writable}
                  onClick={() =>
                    notify({
                      tone: 'success',
                      title: 'Budget saved',
                      body: `${domain.name} will refuse new runs above $${budget}.`,
                    })
                  }
                >
                  Save budget
                </Button>
              </div>
            </div>
          </CardBody>
        </Card>

        <Card>
          <CardHeader
            eyebrow="Access"
            title="Your permissions here"
            description="What your actor is allowed to do in this domain."
          />
          <CardBody>
            <div style={{ display: 'flex', gap: 'var(--space-2)', flexWrap: 'wrap' }}>
              {capabilities.map((capability) => (
                <Badge key={capability} tone="accent">
                  {capability}
                </Badge>
              ))}
            </div>
          </CardBody>
        </Card>
      </div>
    </>
  );
}
