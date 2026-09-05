import { expect, test } from '@playwright/test';

/**
 * **Save budget** used to emit a success toast naming the new limit and issue
 * zero requests (`bean:0141`). A spend cap is the one control in this product
 * whose failure costs money, and the screen reported success for a cap that was
 * never set — worse than an unwired control, because an unwired control is
 * visibly unwired and a false confirmation gives the operator no reason to check.
 *
 * There is no write endpoint to wire it to, so the ruling is the other honest
 * option: the control is disabled and says why.
 *
 * The selectors here are the ones a user has — a role and a label, never a test
 * id — so the same assertions run unchanged against the code before the fix.
 * That is what makes them observable failing rather than only observable
 * passing, and the order is chosen so the first failure names the defect: the
 * toast, not the missing attribute.
 */
test('clicking Save budget confirms nothing, because nothing is saved', async ({ page }) => {
  const writes: string[] = [];
  page.on('request', (request) => {
    if (request.url().includes('/api/') && request.method() !== 'GET') {
      writes.push(`${request.method()} ${request.url()}`);
    }
  });

  await page.goto('/domains/modus/settings');

  // Clicked past the actionability check: the assertion is about what the app
  // says, not about whether Playwright would let a user reach the control.
  const save = page.getByRole('button', { name: 'Save budget' });
  await save.click({ force: true });

  /*
    Not `expect(...).toHaveCount(0)`, and this is the whole point of the test.

    A web-first assertion retries until it holds, and a toast dismisses itself
    after six seconds (`DISMISS_AFTER_MS`, backoffice/src/ui/Toast.tsx). So
    `toHaveCount(0)` sat through the false confirmation, watched it disappear on
    its own, and reported a pass — against the very code that raised it. The
    question here is whether a confirmation was ever shown, not whether it is
    still on screen, and only a non-retrying read asks that question.

    The wait is the other half: it gives the app the time it needs to raise one.
    Measured at 1 immediately after `click()` resolves, so 250ms is margin, not
    hope.
  */
  await page.waitForTimeout(250);
  expect(await page.getByText('Budget saved').count()).toBe(0);
  expect(await page.getByRole('status', { name: 'Notifications' }).innerText()).toBe('');
  expect(writes).toEqual([]);

  // ...and the control is honest about why, rather than silently inert.
  await expect(save).toBeDisabled();
  await expect(page.getByTestId('settings-readonly')).toBeVisible();
});

/** Criterion 2 of `bean:0141`: these either submit, or they are not editable. */
test('display name and environment are not editable', async ({ page }) => {
  await page.goto('/domains/modus/settings');

  await expect(page.getByLabel('Display name')).toBeDisabled();
  await expect(page.getByLabel('Environment')).toBeDisabled();
  await expect(page.getByLabel('Budget (USD)')).toBeDisabled();

  // Still a display of the real configuration, not a blank.
  await expect(page.getByLabel('Display name')).toHaveValue('Modus Core');
  await expect(page.getByLabel('Budget (USD)')).toHaveValue('750');
});
