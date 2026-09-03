import { expect, test } from '@playwright/test';
import type { Page } from '@playwright/test';

/** The mock transport takes its pacing from the URL, so the replay runs fast here. */
const CONSOLE = '/domains/modus/agents?replay=0.04';

/** `replay=0` collapses every delay to zero, so a whole session settles at once. */
const INSTANT = '/domains/modus/agents?replay=0';

function parseUsd(text: string): number {
  return Number(text.replace(/[^0-9.]/g, ''));
}

/** Runs the canned session to completion on `model` and returns what it cost. */
async function costOfOneSession(page: Page, model: string): Promise<number> {
  await page.getByLabel('Model').selectOption(model);
  await page.getByTestId('agent-run').click();
  await expect(page.getByText('Complete').first()).toBeVisible({ timeout: 20_000 });
  return parseUsd(await page.getByTestId('agent-cost').innerText());
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
  await expect(transcript.getByText(/StreamTransport, StreamEvent, BASE_RATES_UPM/)).toBeVisible();

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

/**
 * The cost figure is the point of this screen, so it has to be *priced*, not
 * merely non-zero. The same prompt produces the same token counts on every
 * model, so the ratio between two sessions is exactly the ratio between two
 * rates: Opus 5 ($5/$25 per MTok) is 5x Haiku 4.5 ($1/$5), and Sonnet 5 is 2x
 * on its introductory $2/$10. Mispricing ONE model relative to the others moves
 * its ratio, and that is what this catches: an Opus 5 entry of $15/$75 would
 * read about 15x here instead of 5x, and fail.
 *
 * That $15/$75 is not hypothetical, and this test exists because of it.
 * `bean:0002` records it under `## Review cycle 1`: Opus 5 shipped at $15/$75 in
 * PR #3 — 3x over on the console's default model — and was caught **in review**.
 * This test was then written as the regression guard, and that bean pre-computes
 * its behaviour: "Restoring 15/75 makes that ratio 14.95 and the test fails."
 *
 * "About 15x" above rather than 15x exactly, and 14.95 attributed to `bean:0002`
 * rather than restated as current: the rates are an exact 15x multiple, but the
 * cost is floored per message and per token kind, which takes proportionally
 * more from the smaller figure and pulls the ratio just under. 14.95 is that
 * bean's measurement under the pricing model of its day — two token kinds and
 * floating-point dollars — so it is cited as its figure, not reproduced as ours.
 * The assertion tolerance is what makes this immaterial, and stating a bare 15x
 * beside a quoted 14.95 was a contradiction seven lines wide.
 *
 * One correction worth keeping, because two readers got it wrong in the same
 * way. An earlier version of this comment said the *test* caught the error; a
 * reviewer did, and the test came after. Checking that claim against `git log`
 * makes it look fabricated — Opus 5 is $5/$25 in every commit that ever touched
 * `transport.ts`, and `git log --all -S` finds $15/$75 nowhere — because the fix
 * landed before the merge. **A defect caught in review is invisible to committed
 * history by construction**, which is exactly why this project keeps its review
 * record in `.beans/` (`adr:0005-evidence-lives-in-the-work-item`). Search there
 * before concluding an event did not happen.
 *
 * **Read what this does NOT cover.** It prices the mock's tokens from
 * `BASE_RATES_UPM` and then asserts a ratio derived from `BASE_RATES_UPM`: it
 * compares the code to itself. It catches a table that is internally
 * inconsistent — which is the defect it was written for — and it can never
 * catch one that is merely *stale*. Sonnet 5's introductory rate lapses after
 * 2026-08-31, at which point the table is 33% low and this test stays green.
 * `bean:0090` carries that gap; nothing in the repository compares these
 * numbers to `doc:60-cost-model#price-book`.
 */
test('the session cost is priced from the rate of the model that ran', async ({ page }) => {
  await page.goto(INSTANT);

  const haiku = await costOfOneSession(page, 'claude-haiku-4-5');
  expect(haiku).toBeGreaterThan(0);

  await page.getByRole('button', { name: 'Clear' }).click();
  const sonnet = await costOfOneSession(page, 'claude-sonnet-5');

  await page.getByRole('button', { name: 'Clear' }).click();
  const opus = await costOfOneSession(page, 'claude-opus-5');

  expect(opus / haiku).toBeCloseTo(5, 1);
  expect(sonnet / haiku).toBeCloseTo(2, 1);
});

/**
 * `keepLargerFrame` discards the losing frame of a repeated `messageId` on the
 * premise that frames of one message agree on the four non-output token kinds.
 * The premise holds across the measured corpus and **nothing asserts it** — the
 * Python replay renders a count into a table and never fails on it. So the one
 * place it is checked is the console, and a check nobody has watched fail is
 * not a check (`doc:00-constitution#observed-failing`). This is that watching.
 */
test('a usage frame that disagrees on cache tokens is reported, not discarded', async ({
  page,
}) => {
  await page.goto('/domains/modus/agents?replay=0&fault=usage-disagreement');
  await page.getByTestId('agent-run').click();

  const transcript = page.getByTestId('agent-transcript');
  const notice = transcript.getByText(/disagree on input or cache tokens/);

  // Exactly one, for the message that disagreed. Every later frame of that
  // message disagrees with the frame that was retained, so a per-frame notice
  // would repeat — which is what the first run of this test caught.
  await expect(notice).toHaveCount(1);
  await expect(notice).toBeVisible();

  // The run is not derailed by it: the notice is a report, not a terminal state.
  await expect(page.getByText('Complete').first()).toBeVisible({ timeout: 20_000 });
});

/**
 * Block ids share one namespace across kinds, and a tool block's id is the
 * producer's `callId` — external input against a real transport. A suppression
 * check that matches on id alone can therefore be silenced by a producer that
 * picks the notice's id, and the detector reports nothing while the run looks
 * healthy. Failing open is the one way this must not fail, so it is watched.
 */
test('a tool id colliding with the notice id does not suppress the detector', async ({ page }) => {
  await page.goto('/domains/modus/agents?replay=0&fault=usage-disagreement-collision');
  await page.getByTestId('agent-run').click();

  const transcript = page.getByTestId('agent-transcript');
  await expect(transcript.getByText(/disagree on input or cache tokens/)).toHaveCount(1);
  await expect(page.getByText('Complete').first()).toBeVisible({ timeout: 20_000 });
});

/**
 * `messageId` is producer-controlled wire data, and the usage map is a plain
 * object. So an id naming an inherited property is a lookup *hit* on a map that
 * has never seen that message.
 *
 * Two failures follow silently, and the second is the expensive one: a
 * disagreement is invented on a stream containing none, and `keepLargerFrame`
 * compares against the inherited value, keeps it, and never writes the message
 * into the map — so its tokens vanish from the fold and the run's cost and peak
 * context are both understated with nothing on screen to say so.
 *
 * The stream here is otherwise untouched: same frames, same counts, no
 * disagreement. Only one id is renamed, which any producer may legitimately do.
 */
test('a message id naming an inherited property is counted, not silently dropped', async ({
  page,
}) => {
  await page.goto('/domains/modus/agents?replay=0&fault=proto-message-id');
  await page.getByTestId('agent-run').click();
  await expect(page.getByText('Complete').first()).toBeVisible({ timeout: 20_000 });

  // No disagreement exists in this stream, so none may be reported.
  await expect(
    page.getByTestId('agent-transcript').getByText(/disagree on input or cache tokens/),
  ).toHaveCount(0);

  // And the renamed message is still in the fold. Compare against the identical
  // clean run: an equal cost proves nothing was dropped, where the pre-fix code
  // silently halved it.
  const renamedCost = await page.getByTestId('agent-cost').innerText();
  const renamedOut = await page.getByTestId('agent-tokens-out').innerText();

  await page.goto(INSTANT);
  await page.getByTestId('agent-run').click();
  await expect(page.getByText('Complete').first()).toBeVisible({ timeout: 20_000 });

  expect(parseUsd(renamedCost)).toBeCloseTo(
    parseUsd(await page.getByTestId('agent-cost').innerText()),
    6,
  );
  expect(renamedOut).toBe(await page.getByTestId('agent-tokens-out').innerText());
});

/** The happy path must NOT trip the detector, or the notice means nothing. */
test('an ordinary session reports no frame disagreement', async ({ page }) => {
  await page.goto(INSTANT);
  await page.getByTestId('agent-run').click();
  await expect(page.getByText('Complete').first()).toBeVisible({ timeout: 20_000 });
  await expect(
    page.getByTestId('agent-transcript').getByText(/disagree on input or cache tokens/),
  ).toHaveCount(0);
});

test('a running session can be stopped', async ({ page }) => {
  await page.goto('/domains/modus/agents?replay=1');

  await page.getByTestId('agent-run').click();
  await expect(page.getByTestId('agent-stop')).toBeVisible();

  // The mock closes the way a real EventSource does — no synthetic session-end —
  // so reaching 'Cancelled' proves the console owns the transition itself.
  await page.getByTestId('agent-stop').click();
  await expect(page.getByText('Cancelled').first()).toBeVisible();
  await expect(page.getByTestId('agent-run')).toBeVisible();
});

/**
 * A stream that dies mid tool call never sends `session-end` — the server did
 * not live long enough to send one. The tool block has to resolve anyway.
 */
test('an error mid tool call resolves the tool instead of spinning forever', async ({ page }) => {
  await page.goto('/domains/modus/agents?replay=0.04&fault=stream-error');
  await page.getByTestId('agent-run').click();

  const transcript = page.getByTestId('agent-transcript');
  await expect(transcript.getByText('Read', { exact: true })).toBeVisible();
  await expect(transcript.getByText(/The model stream dropped mid tool call/)).toBeVisible();

  await expect(transcript.getByText('running', { exact: true })).toHaveCount(0);
  await expect(transcript.getByText('failed', { exact: true })).toBeVisible();
  await expect(transcript.getByText('Interrupted')).toBeVisible();

  // Terminal: the composer comes back rather than leaving a dead Stop button.
  await expect(page.getByText('Error').first()).toBeVisible();
  await expect(page.getByTestId('agent-run')).toBeVisible();
});

/** The same requirement for a dropped connection, which arrives via `onError`. */
test('a dropped connection is reported and resolves the tool in flight', async ({ page }) => {
  await page.goto('/domains/modus/agents?replay=0.04&fault=transport-error');
  await page.getByTestId('agent-run').click();

  const transcript = page.getByTestId('agent-transcript');
  await expect(transcript.getByText(/connection to the agent service dropped/)).toBeVisible();

  await expect(transcript.getByText('running', { exact: true })).toHaveCount(0);
  await expect(transcript.getByText('Interrupted')).toBeVisible();
  await expect(page.getByTestId('agent-run')).toBeVisible();
});

test('runs are refused where the actor cannot start them', async ({ page }) => {
  // Beacon Analytics grants agents.read without agents.run: this actor may read
  // the console and may not spend money in it.
  await page.goto('/domains/beacon/agents');

  const run = page.getByTestId('agent-run');
  await expect(run).toBeVisible();
  await expect(run).toBeDisabled();
  await expect(page.getByText('You cannot start runs in this domain')).toBeVisible();

  // The gate holds under a forced click: nothing starts.
  await run.click({ force: true });
  await expect(page.getByTestId('agent-transcript').getByText('Nothing running')).toBeVisible();
  await expect(page.getByTestId('agent-cost')).toHaveText('$0.0000');
  await expect(page.getByText('Streaming')).toHaveCount(0);
});

test('a domain with no run history says so', async ({ page }) => {
  await page.goto('/domains/sandbox/agents');
  await expect(page.getByText('No runs recorded')).toBeVisible();
  // The sandbox does grant agents.run, so the button is live here.
  await expect(page.getByTestId('agent-run')).toBeEnabled();
});
