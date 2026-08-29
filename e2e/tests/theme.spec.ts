import { expect, test } from '@playwright/test';

const themeAttribute = 'data-theme';

test.describe('theme', () => {
    test.use({ colorScheme: 'light' });

    test('toggles, repaints and persists', async ({ page }) => {
        await page.goto('/domains/modus/work');

        const html = page.locator('html');
        // "System" is the default, and it stamps nothing at all.
        await expect(html).not.toHaveAttribute(themeAttribute, /.+/);

        const lightBackground = await page.evaluate(
            () => getComputedStyle(document.body).backgroundColor,
        );

        await page.getByTestId('theme-toggle').click();
        await expect(html).toHaveAttribute(themeAttribute, 'dark');

        const darkBackground = await page.evaluate(
            () => getComputedStyle(document.body).backgroundColor,
        );
        expect(darkBackground).not.toBe(lightBackground);

        // The choice survives a reload, applied before first paint.
        await page.reload();
        await expect(html).toHaveAttribute(themeAttribute, 'dark');

        await page.getByTestId('theme-toggle').click();
        await expect(html).toHaveAttribute(themeAttribute, 'light');
    });

    test('an explicit light choice beats a dark operating system', async ({ browser }) => {
        const context = await browser.newContext({ colorScheme: 'dark' });
        const page = await context.newPage();
        await page.goto('/domains/modus/work');

        const darkBackground = await page.evaluate(
            () => getComputedStyle(document.body).backgroundColor,
        );

        await page.getByTestId('theme-toggle').click();
        await expect(page.locator('html')).toHaveAttribute(themeAttribute, 'light');

        const lightBackground = await page.evaluate(
            () => getComputedStyle(document.body).backgroundColor,
        );
        expect(lightBackground).not.toBe(darkBackground);

        await context.close();
    });
});
