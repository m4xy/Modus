import { expect, test } from '@playwright/test';

test('switching domain re-scopes the whole app', async ({ page }) => {
    await page.goto('/domains/modus/work');

    await expect(page.getByTestId('current-domain')).toHaveText('Modus Core');
    await expect(page.getByRole('button', { name: /Backoffice foundation/ })).toBeVisible();
    // Modus Core grants cost.read, so the Cost section is present.
    await expect(page.getByTestId('nav-cost')).toBeVisible();

    await page.getByTestId('domain-switcher').click();
    await expect(page.getByTestId('domain-menu')).toBeVisible();
    await page.getByRole('menuitem', { name: /Atlas Ledger/ }).click();

    await expect(page).toHaveURL(/\/domains\/atlas-ledger\/work$/);
    await expect(page.getByTestId('current-domain')).toHaveText('Atlas Ledger');
    await expect(page.getByRole('button', { name: /Migrate ledger postings/ })).toBeVisible();
    await expect(page.getByRole('button', { name: /Backoffice foundation/ })).toHaveCount(0);
});

test('navigation reflects the permissions of the domain you are in', async ({ page }) => {
    await page.goto('/domains/atlas-ledger/work');

    // No cost.read grant here, and Cost is a 'hide' section.
    await expect(page.getByTestId('nav-cost')).toHaveCount(0);
    // Settings is 'hide' too, but this domain does hold settings.read.
    await expect(page.getByTestId('nav-settings')).toBeVisible();

    // Typing the URL directly hits the same wall the navigation does.
    await page.goto('/domains/atlas-ledger/cost');
    await expect(page.getByText('You do not have access to this section')).toBeVisible();
});

test('locked sections say why instead of vanishing', async ({ page }) => {
    await page.goto('/domains/sandbox/work');

    const locked = page.getByTestId('nav-repositories');
    await expect(locked).toHaveAttribute('aria-disabled', 'true');

    // aria-disabled, not disabled: it stays focusable and explains itself.
    await locked.click({ force: true });
    await expect(page.getByRole('status').getByText(/not available in Sandbox/)).toBeVisible();
});

test('the switcher opens from the keyboard and closes on Escape', async ({ page }) => {
    await page.goto('/domains/modus/work');

    await page.getByTestId('domain-switcher').focus();
    await page.keyboard.press('ArrowDown');
    await expect(page.getByTestId('domain-menu')).toBeVisible();

    await page.keyboard.press('Escape');
    await expect(page.getByTestId('domain-menu')).toBeHidden();
    await expect(page.getByTestId('domain-switcher')).toBeFocused();
});
