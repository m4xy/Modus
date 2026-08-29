import AxeBuilder from '@axe-core/playwright';
import { expect, test } from '@playwright/test';
import type { Page } from '@playwright/test';

const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'];

/**
 * Waits for every finite animation to finish before measuring. An element
 * captured mid-fade is composited against whatever is behind it, so contrast is
 * read against a blend that no user ever sees. Infinite animations (the caret,
 * the skeleton shimmer) are skipped — they never settle.
 */
async function settle(page: Page) {
    await page.evaluate(async () => {
        const finite = document
            .getAnimations()
            .filter((animation) => animation.effect?.getComputedTiming().iterations !== Infinity);
        await Promise.all(finite.map((animation) => animation.finished.catch(() => undefined)));
    });
}

async function scan(page: Page) {
    await settle(page);
    return new AxeBuilder({ page }).withTags(TAGS).analyze();
}

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
