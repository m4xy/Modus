import { expect, test } from '@playwright/test';

/**
 * **Save budget** used to emit a success toast naming the new limit and issue
 * zero requests (`bean:0141`). A spend cap is the one control in this product
 * whose failure costs money, and the screen reported success for a cap that was
 * never set — worse than an unwired control, because an unwired control is
 * visibly unwired and a false confirmation gives the operator no reason to check.
 *
 * There is no write endpoint to wire it to, so the ruling is the other honest
 * option: the control is read-only and says why.
 *
 * The selectors here are the ones a user has — a role and a label, never a test
 * id — so the same assertions run unchanged against the code before the fix.
 * That is what makes them observable failing rather than only observable
 * passing, and the order is chosen so the first failure names the defect: the
 * toast, not a missing attribute.
 */
test('clicking Save budget confirms nothing, because nothing is saved', async ({ page }) => {
  const writes: string[] = [];
  page.on('request', (request) => {
    if (request.url().includes('/api/') && request.method() !== 'GET') {
      writes.push(`${request.method()} ${request.url()}`);
    }
  });

  await page.goto('/domains/modus/settings');

  const save = page.getByRole('button', { name: 'Save budget' });
  await save.click({ force: true });

  /*
    Not `expect(...).toHaveCount(0)`, and this is the whole point of the test.

    A web-first assertion retries until it holds, and a toast dismisses itself
    after six seconds (`DISMISS_AFTER_MS`, backoffice/src/ui/Toast.tsx). So
    `toHaveCount(0)` sat through the false confirmation, watched it disappear on
    its own at 6398ms, and reported a pass — against the very code that raised
    it. The question here is whether a confirmation was ever shown, not whether
    it is still on screen, and only a non-retrying read asks that question.

    The wait is the other half: it gives the app the time it needs to raise one.
    Measured at 1 immediately after `click()` resolves, so 250ms is margin, not
    hope (`bean:0190`).
  */
  await page.waitForTimeout(250);
  expect(await page.getByText('Budget saved').count()).toBe(0);
  expect(await page.getByRole('status', { name: 'Notifications' }).innerText()).toBe('');
  expect(writes).toEqual([]);
});

/**
 * The inert state has to be reachable and self-explaining, not merely inert.
 *
 * The first version of this fix used hard `disabled` on all five controls, and a
 * keyboard sweep found that tab order never reached any of them: a screen reader
 * user could not find the fields, let alone be told why they did nothing. The
 * repository already stated the right answer in `domain-switcher.spec.ts` —
 * "aria-disabled, not disabled: it stays focusable and explains itself" — and
 * this screen was doing the opposite of its own precedent.
 */
test('every read-only control is reachable and says why it is read-only', async ({ page }) => {
  await page.goto('/domains/modus/settings');

  const fields = [
    { label: 'Display name', value: 'Modus Core' },
    { label: 'Environment', value: 'production' },
    { label: 'Budget (USD)', value: '750' },
  ];

  for (const field of fields) {
    const control = page.getByLabel(field.label);

    // Read-only in fact: it holds the real value and refuses input.
    await expect(control).toHaveValue(field.value);
    await expect(control).toHaveAttribute('readonly', '');
    await expect(control).toHaveAttribute('aria-disabled', 'true');

    // Reachable: a hard-disabled control is skipped by the keyboard entirely.
    await control.focus();
    await expect(control).toBeFocused();

    // ...and it explains itself where assistive tech will read it.
    const describedBy = await control.getAttribute('aria-describedby');
    expect(describedBy, `${field.label} has no aria-describedby`).toBeTruthy();
    await expect(page.locator(`#${describedBy}`)).toContainText('no endpoint to save it to');
  }

  const save = page.getByRole('button', { name: 'Save budget' });
  await expect(save).toHaveAttribute('aria-disabled', 'true');
  await save.focus();
  await expect(save).toBeFocused();

  const saveNote = await save.getAttribute('aria-describedby');
  expect(saveNote, 'Save budget has no aria-describedby').toBeTruthy();
  await expect(page.locator(`#${saveNote}`)).toContainText('until there is a server to save to');
});

/** Criterion 2 of `bean:0141`: these either submit, or they are not editable. */
test('typing into display name and environment changes nothing', async ({ page }) => {
  await page.goto('/domains/modus/settings');

  for (const label of ['Display name', 'Environment', 'Budget (USD)']) {
    const control = page.getByLabel(label);
    const before = await control.inputValue();
    await control.focus();
    await page.keyboard.type('zzz');
    expect(await control.inputValue(), `${label} accepted input`).toBe(before);
  }
});
