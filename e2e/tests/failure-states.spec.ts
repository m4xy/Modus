import { expect, test } from '@playwright/test';
import type { Page } from '@playwright/test';
import { scan } from './support/axe';

/**
 * A server failure and an empty collection are different facts, and five screens
 * rendered them identically (`bean:0140`): a route that branches only on
 * `isPending` falls through to its empty state when the query rejects, and the
 * agent console's run history read no status flag at all, so it got there one
 * step sooner.
 *
 * `?fail=<resource>` is the mocked API's fault switch
 * (`backoffice/src/mocks/handlers.ts`). It exists because MSW's service worker
 * answers before the request reaches the network, so `page.route` cannot fulfil
 * these calls with a 500 — the branch is unreachable from a test without it.
 *
 * Each screen is asserted twice, and the second half is the one that makes the
 * first mean anything: a mechanism that fires on every input has not
 * distinguished the two cases, it has only replaced one wrong answer with
 * another.
 */
/**
 * Waits for the route to have mounted and its query to have stopped loading,
 * before anything asserts an ABSENCE.
 *
 * Without this the absence assertions below are vacuous, and were: run straight
 * after `goto`, `toHaveCount(0)` resolves on the first poll, while the screen is
 * still a skeleton and the empty-state sentence has not been rendered yet. Every
 * one of them passed against the unfixed code — the shape
 * `doc:00-constitution#observed-failing` names, where a check that examined
 * nothing and a check that passed print the same thing.
 *
 * `aria-busy="true"` is `SkeletonList`'s own marker, so this settles on the
 * component the routes actually use rather than on a timeout.
 */
async function settled(page: Page, heading: string): Promise<void> {
  await expect(page.getByRole('heading', { level: 1, name: heading })).toBeVisible();
  await expect(page.locator('[aria-busy="true"]')).toHaveCount(0);
}

const screens = [
  {
    name: 'Work',
    heading: 'Work',
    path: '/domains/modus/work',
    resource: 'work',
    failure: 'Work items could not be loaded',
    empty: 'No work items yet',
    healthy: /Backoffice foundation/,
  },
  {
    name: 'Repositories',
    heading: 'Repositories',
    path: '/domains/modus/repositories',
    resource: 'repositories',
    failure: 'Repositories could not be loaded',
    empty: 'No repositories connected',
    healthy: /git@github\.com:m4xy\/Modus\.git/,
  },
  {
    name: 'Memories',
    heading: 'Memories',
    path: '/domains/modus/memories',
    resource: 'memories',
    failure: 'Memories could not be loaded',
    empty: 'Nothing remembered yet',
    healthy: /tokens per run/,
  },
  {
    name: 'Skills',
    heading: 'Skills',
    path: '/domains/modus/skills',
    resource: 'skills',
    failure: 'Skills could not be loaded',
    empty: 'No skills installed',
    healthy: /Invocations \(30d\)/,
  },
  {
    name: 'Agent console run history',
    heading: 'Agent console',
    path: '/domains/modus/agents',
    resource: 'runs',
    failure: 'Run history could not be loaded',
    empty: 'No runs recorded',
    healthy: /run_301/,
  },
  {
    name: 'Cost',
    heading: 'Cost',
    path: '/domains/modus/cost',
    resource: 'cost',
    failure: 'Cost data could not be loaded',
    empty: 'No attributed spend yet',
    healthy: /Daily spend/,
  },
] as const;

for (const screen of screens) {
  test(`${screen.name} reports a failed read as a failure, not as no data`, async ({ page }) => {
    await page.goto(`${screen.path}?fail=${screen.resource}`);

    await settled(page, screen.heading);

    // Asserted first, and in the user's own words, because this is the defect:
    // the claim the screen must never make when it has not read anything.
    await expect(page.getByText(screen.empty)).toHaveCount(0);

    const alert = page.getByRole('alert');
    await expect(alert).toContainText(screen.failure);
    // The failure is announced, not merely drawn.
    await expect(alert).toHaveAttribute('data-testid', 'error-state');
  });

  test(`${screen.name} still renders its data when the read succeeds`, async ({ page }) => {
    await page.goto(screen.path);

    await expect(page.getByText(screen.healthy).first()).toBeVisible();
    await expect(page.getByRole('alert')).toHaveCount(0);
  });
}

/**
 * The tiles above the Work table read `0`, `$0.00` and `0` under a 500 — three
 * measurements nobody took, sitting above the sentence that says the backlog is
 * empty. Criterion 2 of `bean:0140`: no summary figure derived from a failed
 * query is rendered as a measured value.
 */
test('Work reports no figures it did not measure', async ({ page }) => {
  await page.goto('/domains/modus/work?fail=work');
  await settled(page, 'Work');

  await expect(page.getByText('$0.00')).toHaveCount(0);
  await expect(page.getByText('Open items')).toHaveCount(0);
  await expect(page.getByText('Spend attributed')).toHaveCount(0);
  await expect(page.getByText('In review')).toHaveCount(0);

  // Nor a filter over a collection that was never fetched.
  await expect(page.getByLabel('Search work items')).toHaveCount(0);

  await expect(page.getByRole('alert')).toBeVisible();
});

/**
 * The console's stream is its own transport and does not go through the query
 * that failed, so a failed history must not take the composer down with it.
 */
test('a failed run history leaves the console usable', async ({ page }) => {
  await page.goto('/domains/modus/agents?fail=runs');
  await settled(page, 'Agent console');

  await expect(page.getByText('No runs recorded')).toHaveCount(0);
  await expect(page.getByRole('alert')).toBeVisible();
  await expect(page.getByTestId('agent-run')).toBeEnabled();
});

/** The boot path already had this branch; this is the regression guard on it. */
test('a failed session says the API is unreachable', async ({ page }) => {
  await page.goto('/domains/modus/work?fail=session');

  await expect(page.getByText('Cannot reach the Modus API')).toBeVisible();
  await expect(page.getByText('No work items yet')).toHaveCount(0);
});

/**
 * The error state is new UI on the six screens that read a collection and axe was clean on all of them
 * before it existed. Assumed-clean is not clean.
 */
test('a failed screen has no accessibility violations, light and dark', async ({ page }) => {
  await page.goto('/domains/modus/work?fail=work');
  await settled(page, 'Work');
  await expect(page.getByRole('alert')).toBeVisible();

  expect((await scan(page)).violations).toEqual([]);

  await page.getByTestId('theme-toggle').click();
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark');

  expect((await scan(page)).violations).toEqual([]);
});
