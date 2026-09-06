import { expect, test } from '@playwright/test';
import { scan } from './support/axe';

/**
 * Every navigable surface, the heading that proves the right one rendered, and
 * an axe scan of it. One visit, both assertions: a separate smoke test per
 * route only ever failed alongside this one.
 */
const routes = [
  { path: '/domains/modus/work', heading: 'Work' },
  { path: '/domains/modus/repositories', heading: 'Repositories' },
  { path: '/domains/modus/agents', heading: 'Agent console' },
  { path: '/domains/modus/memories', heading: 'Memories' },
  { path: '/domains/modus/cost', heading: 'Cost' },
  { path: '/domains/modus/skills', heading: 'Skills' },
  { path: '/domains/modus/settings', heading: 'Settings' },
];

for (const route of routes) {
  test(`${route.heading} renders with no accessibility violations`, async ({ page }) => {
    await page.goto(route.path);
    await expect(page.getByRole('heading', { level: 1, name: route.heading })).toBeVisible();
    await expect(page.getByTestId('domain-switcher')).toBeVisible();

    const results = await scan(page);
    expect(results.violations).toEqual([]);
  });
}

test('no accessibility violations in dark theme', async ({ page }) => {
  await page.goto('/domains/modus/cost');
  await page.getByTestId('theme-toggle').click();
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark');

  const results = await scan(page);
  expect(results.violations).toEqual([]);
});

test('no accessibility violations with the domain menu open', async ({ page }) => {
  await page.goto('/domains/modus/work');
  await page.getByTestId('domain-switcher').click();
  await expect(page.getByTestId('domain-menu')).toBeVisible();

  const results = await scan(page);
  expect(results.violations).toEqual([]);
});

test('no accessibility violations in an open dialog', async ({ page }) => {
  await page.goto('/domains/modus/work');
  await page.getByRole('button', { name: /Backoffice foundation/ }).click();
  await expect(page.getByRole('dialog')).toBeVisible();

  const results = await scan(page);
  expect(results.violations).toEqual([]);
});

test('no accessibility violations mid-stream in the console', async ({ page }) => {
  await page.goto('/domains/modus/agents?replay=0.2');
  await page.getByTestId('agent-run').click();
  await expect(
    page.getByTestId('agent-transcript').getByText(/Reading the transport seam/),
  ).toBeVisible();

  const results = await scan(page);
  expect(results.violations).toEqual([]);
});

/**
 * Settings in dark, specifically for the inert-but-focusable controls.
 *
 * A hard-`disabled` control is exempt from WCAG 1.4.3, so the pale primary
 * "Save budget" was never measured by any of the scans above — zero violations
 * on this route was a weaker signal than it read as. Now that the control is
 * `aria-disabled` and stays in the tab order, axe measures it, and this is the
 * theme where a muted-token treatment is most likely to fall short.
 */
test('no accessibility violations on the read-only settings controls in dark', async ({ page }) => {
  await page.goto('/domains/modus/settings');
  await page.getByTestId('theme-toggle').click();
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark');

  const save = page.getByRole('button', { name: 'Save budget' });
  await expect(save).toHaveAttribute('aria-disabled', 'true');
  await save.focus();

  const results = await scan(page);
  expect(results.violations).toEqual([]);
});
