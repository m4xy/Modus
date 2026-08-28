import { expect, test } from '@playwright/test';

/** The mock transport takes its pacing from the URL, so the replay runs fast here. */
const CONSOLE = '/domains/modus/agents?replay=0.04';

function parseUsd(text: string): number {
  return Number(text.replace(/[^0-9.]/g, ''));
}

test('streams assistant text, tool calls and results incrementally', async ({ page }) => {
  await page.goto(CONSOLE);

  const transcript = page.getByTestId('agent-transcript');
  await expect(transcript.getByText('Nothing running')).toBeVisible();

  await page.getByTestId('agent-prompt').fill('Swap the mock transport for a real SSE client.');
  await page.getByTestId('agent-run').click();

  // The prompt is echoed straight away, before any model output exists.
  await expect(
    transcript.getByText('Swap the mock transport for a real SSE client.'),
  ).toBeVisible();
  await expect(page.getByText('Streaming').first()).toBeVisible();

  // Assistant text arrives in pieces rather than in one block.
  await expect(transcript.getByText(/Reading the transport seam/)).toBeVisible();
  const firstLength = (await transcript.innerText()).length;
  await expect
    .poll(async () => (await transcript.innerText()).length, { timeout: 15_000 })
    .toBeGreaterThan(firstLength);

  // Tool calls render as their own blocks, with a result attached.
  await expect(transcript.getByText('Read', { exact: true })).toBeVisible();
  await expect(transcript.getByText('backoffice/src/agent/transport.ts')).toBeVisible();
  await expect(transcript.getByText(/StreamTransport, StreamEvent, PRICING/)).toBeVisible();

  // A failing tool is shown as failed, not swallowed.
  await expect(transcript.getByText(/exit 1 · 1 error/)).toBeVisible({ timeout: 20_000 });

  await expect(page.getByText('Complete').first()).toBeVisible({ timeout: 20_000 });
});

test('the cost counter climbs while the session runs', async ({ page }) => {
  await page.goto(CONSOLE);

  const cost = page.getByTestId('agent-cost');
  await expect(cost).toHaveText('$0.0000');

  await page.getByTestId('agent-run').click();

  await expect.poll(async () => parseUsd(await cost.innerText())).toBeGreaterThan(0);
  const early = parseUsd(await cost.innerText());

  await expect
    .poll(async () => parseUsd(await cost.innerText()), { timeout: 20_000 })
    .toBeGreaterThan(early);

  await expect
    .poll(async () =>
      Number((await page.getByTestId('agent-tokens-out').innerText()).replace(/[^0-9.]/g, '')),
    )
    .toBeGreaterThan(0);
});

test('a running session can be stopped', async ({ page }) => {
  await page.goto('/domains/modus/agents?replay=1');

  await page.getByTestId('agent-run').click();
  await expect(page.getByTestId('agent-stop')).toBeVisible();

  await page.getByTestId('agent-stop').click();
  await expect(page.getByText('Cancelled').first()).toBeVisible();
  await expect(page.getByTestId('agent-run')).toBeVisible();
});

test('runs are refused where the actor cannot start them', async ({ page }) => {
  // Atlas Ledger grants agents.read and agents.run; the sandbox grants both too,
  // so the console is exercised where history is empty instead.
  await page.goto('/domains/sandbox/agents');
  await expect(page.getByText('No runs recorded')).toBeVisible();
});
