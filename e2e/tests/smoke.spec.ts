import { expect, test } from '@playwright/test';

/** Every navigable surface, and the heading that proves it actually rendered. */
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
  test(`renders ${route.heading}`, async ({ page }) => {
    await page.goto(route.path);
    await expect(page.getByRole('heading', { level: 1, name: route.heading })).toBeVisible();
    await expect(page.getByTestId('domain-switcher')).toBeVisible();
  });
}

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

test('cost surfaces spend by stage and by model', async ({ page }) => {
  await page.goto('/domains/modus/cost');
  await expect(page.getByTestId('cost-hero')).toHaveText('$428.60');
  await expect(page.getByRole('heading', { name: 'By stage' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'By model' })).toBeVisible();
  await expect(page.getByRole('img', { name: /Spend split by model/ })).toBeVisible();
});
