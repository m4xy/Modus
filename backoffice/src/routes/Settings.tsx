import { useDomain } from '../app/DomainContext';
import { PageHeader } from '../components/PageHeader';
import { Badge, Button, Card, CardBody, CardHeader, Input, Select } from '../ui';

/**
 * Every control on this screen is read-only, and that is the fix rather than a
 * gap in it.
 *
 * There is no endpoint to call. `src/api/client.ts` declares reads only, and the
 * aggregate that would accept a settings change is not built yet (`bean:0018`).
 * What stood here was worse than an unwired control: **Save budget**'s whole
 * handler was a call to `notify`, so the operator was told in a success toast
 * that a spend cap had been set which had never left the browser (`bean:0141`).
 * An unwired control is visibly unwired; a false confirmation is
 * indistinguishable from a real one, so nobody goes and checks.
 *
 * A disabled control that says why is honest. A success toast for a no-op is
 * not. Wiring these to an invented endpoint would only move the untruth one
 * layer down, so they stay disabled until there is a real one to call.
 */
const NO_WRITE_API =
  'Settings cannot be saved yet: this build has no endpoint to write them to, so every control below shows the current configuration and is disabled. Nothing typed here would be kept.';

export function Settings() {
  const { domain, can, capabilities } = useDomain();
  const permitted = can('settings.write');

  return (
    <>
      <PageHeader
        eyebrow={`${domain.id} · settings`}
        title="Settings"
        description="Configuration for this domain. Changes apply to every actor and every run in it."
        actions={
          permitted ? (
            <Badge tone="warn">Read only — settings cannot be saved yet</Badge>
          ) : (
            <Badge tone="warn">Read only — you cannot change settings here</Badge>
          )
        }
      />

      <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-5)' }}>
        <Card>
          <CardBody>
            <p
              data-testid="settings-readonly"
              style={{ color: 'var(--ink-2)', fontSize: 'var(--text-sm)', maxWidth: '68ch' }}
            >
              {NO_WRITE_API}
            </p>
          </CardBody>
        </Card>

        <Card>
          <CardHeader
            eyebrow="Identity"
            title="Domain"
            description="The tenant root every URL in this app hangs off."
          />
          <CardBody>
            <div style={{ display: 'grid', gap: 'var(--space-4)', maxWidth: '32rem' }}>
              <Input label="Domain id" value={domain.id} readOnly disabled />
              <Input label="Display name" value={domain.name} readOnly disabled />
              <Select
                label="Environment"
                value={domain.environment}
                disabled
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
                value={String(domain.monthlyBudgetUsd)}
                readOnly
                disabled
                hint="Applies to model spend only. Infrastructure is billed separately."
              />
              <div>
                <Button variant="primary" disabled data-testid="save-budget">
                  Save budget
                </Button>
                <p
                  style={{
                    marginTop: 'var(--space-2)',
                    color: 'var(--ink-2)',
                    fontSize: 'var(--text-sm)',
                    maxWidth: '48ch',
                  }}
                >
                  Disabled until there is a server to save to. A cap that costs money when it is
                  wrong is not one to confirm on trust.
                </p>
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
