import { useId } from 'react';
import { useDomain } from '../app/DomainContext';
import { PageHeader } from '../components/PageHeader';
import { Badge, Button, Card, CardBody, CardHeader, Input } from '../ui';

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
 * layer down, so they stay read-only until there is a real one to call.
 *
 * **Read-only here means `readOnly` and `aria-disabled`, not `disabled`.** The
 * first version used hard `disabled` throughout, and a keyboard sweep found the
 * result: tab order ran skip link → switcher → seven nav links → actor menu →
 * theme toggle → wrap, reaching not one of the five controls. A screen reader
 * user could not find the fields, and could not have been told why they were
 * inert if they had. The rail already had the right answer — its locked sections
 * are `aria-disabled` precisely so they stay focusable and explain themselves —
 * and this screen was doing the opposite of its own repository's precedent.
 *
 * Every field now carries a `hint`, which `FieldShell` wires to
 * `aria-describedby`, so the reason is announced with the control rather than
 * sitting beside it as unassociated prose.
 */
const NO_WRITE_API = 'Read-only: there is no endpoint to save it to yet, so nothing here is kept.';

export function Settings() {
  const { domain, can, capabilities } = useDomain();
  const permitted = can('settings.write');
  const saveNoteId = useId();

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
              Settings cannot be saved yet: this build has no endpoint to write them to, so every
              control below shows the current configuration and accepts no input. Nothing typed here
              would be kept.
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
              <Input
                label="Domain id"
                value={domain.id}
                readOnly
                aria-disabled="true"
                hint="Fixed for the life of the domain; it is the tenant root in every URL."
              />
              <Input
                label="Display name"
                value={domain.name}
                readOnly
                aria-disabled="true"
                hint={NO_WRITE_API}
              />
              {/*
                A read-only field rather than a disabled `Select`.

                With no write path, the environment is a fact about the domain,
                not a choice being offered. A `<select>` has no `readOnly`, so the
                options are either genuinely changeable — which is the bean:0141
                defect again, a control that accepts input and keeps none — or
                hard `disabled`, which is unreachable by keyboard and so cannot
                carry its own explanation. Rendering the current value is the only
                one of the three that is both honest and announceable. The choice
                itself comes back with the endpoint.
              */}
              <Input
                label="Environment"
                value={domain.environment}
                readOnly
                aria-disabled="true"
                hint={`One of production, staging or sandbox. ${NO_WRITE_API}`}
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
                aria-disabled="true"
                hint={`Applies to model spend only; infrastructure is billed separately. ${NO_WRITE_API}`}
              />
              <div>
                {/*
                  `aria-disabled`, not `disabled`, and no `onClick` at all: the
                  button stays in the tab order so it can be found and described,
                  and a click on it runs nothing because there is nothing bound to
                  run. Inertness is structural here, not enforced by an attribute.
                */}
                <Button
                  variant="primary"
                  aria-disabled="true"
                  aria-describedby={saveNoteId}
                  data-testid="save-budget"
                >
                  Save budget
                </Button>
                <p
                  id={saveNoteId}
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
