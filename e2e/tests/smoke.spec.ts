import { expect, test } from '@playwright/test';

/**
 * The per-route "it rendered" checks live in `accessibility.spec.ts`, which
 * already visits all seven routes and can assert the heading name in the same
 * pass. Duplicating them here bought a second failure for one break rather than
 * covering anything new.
 */

test('the root sends you to your first domain', async ({ page }) => {
  await page.goto('/');
  await expect(page).toHaveURL(/\/domains\/modus\/work$/);
  await expect(page.getByTestId('current-domain')).toHaveText('Modus Core');
});

test('an unknown domain falls back to one you can see', async ({ page }) => {
  await page.goto('/domains/not-a-tenant/work');
  await expect(page).toHaveURL(/\/domains\/modus\/work$/);
});

test('an unknown path explains itself', async ({ page }) => {
  await page.goto('/nowhere');
  await expect(page.getByText('That page does not exist')).toBeVisible();
});

test('work items open their markdown body', async ({ page }) => {
  await page.goto('/domains/modus/work');
  await page.getByRole('button', { name: /Backoffice foundation/ }).click();

  const dialog = page.getByRole('dialog');
  await expect(dialog).toBeVisible();
  await expect(dialog.getByText('Success criteria')).toBeVisible();

  await page.keyboard.press('Escape');
  await expect(dialog).toBeHidden();
});

/**
 * Every number asserted here is *derived* from the fixture rather than echoed
 * back out of it: the month-on-month delta, its percentage, the share of budget
 * consumed, and one work item's share of the month. Each one breaks if the
 * arithmetic behind it breaks, which "$428.60 equals 428.6" never did.
 */
test('cost surfaces spend by stage and by model', async ({ page }) => {
  await page.goto('/domains/modus/cost');

  // 428.60 this month against 351.05 last month: +$77.55, +22%.
  await expect(page.getByTestId('cost-hero')).toHaveText('$428.60');
  await expect(page.getByText(/▲ \$77\.55 \(22%\)/)).toBeVisible();

  // 428.60 of a 750.00 budget is 57%, reported as text and to assistive tech.
  await expect(page.getByText('57% of $750.00')).toBeVisible();
  const budget = page.getByRole('meter', { name: 'Share of the monthly budget used' });
  await expect(budget).toHaveAttribute('aria-valuenow', '57');

  await expect(page.getByRole('heading', { name: 'By stage' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'By model' })).toBeVisible();
  await expect(page.getByRole('img', { name: /Spend split by model/ })).toBeVisible();

  // 63.90 attributed to bean 0002 is 15% of the month.
  const attributed = page.getByRole('row', { name: /Backoffice foundation/ });
  await expect(attributed).toContainText('$63.90');
  await expect(attributed).toContainText('15%');
});

/**
 * The palette has five validated slots and the file's stated rule is that a
 * colour is never reused. Beacon Analytics reports six models, so this is the
 * case that used to wrap: the sixth slice repeated the first one's colour.
 */
test('a sixth model never reuses a series colour', async ({ page }) => {
  await page.goto('/domains/beacon/cost');

  await expect(page.getByRole('heading', { name: 'By model' })).toBeVisible();
  const segments = page.getByTestId('model-split').locator('> div');
  await expect(segments.first()).toBeVisible();

  const colours = await segments.evaluateAll((nodes) =>
    nodes.map((node) => getComputedStyle(node).backgroundColor),
  );

  // The invariant itself: no slice ever wears another slice's colour.
  expect(new Set(colours).size).toBe(colours.length);
  // Five validated slots, so six models render as four series plus one bucket.
  expect(colours).toHaveLength(5);

  // The tail is bucketed rather than dropped, and it is still accounted for.
  await expect(page.getByText('Other (2 models)').first()).toBeVisible();
  await expect(page.getByRole('row', { name: /Other \(2 models\)/ })).toContainText('$55.90');
});

/**
 * The tooltip's Escape handler sits on the trigger rather than on the wrapper
 * span, which is not interactive and may not carry a key handler (bean:0046).
 * Focus opens it, Escape dismisses it, and the trigger keeps its own name.
 */
test('a tooltip opens on focus and dismisses on Escape', async ({ page }) => {
  await page.goto('/domains/modus/work');

  const toggle = page.getByTestId('theme-toggle');
  await toggle.focus();
  const tip = page.getByRole('tooltip');
  await expect(tip).toBeVisible();
  await expect(toggle).toHaveAttribute('aria-describedby', /.+/);

  await page.keyboard.press('Escape');
  await expect(tip).toBeHidden();
});
