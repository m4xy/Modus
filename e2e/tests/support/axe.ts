import AxeBuilder from '@axe-core/playwright';
import type { Page } from '@playwright/test';

export const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'];

/**
 * Waits for every finite animation to finish before measuring. An element
 * captured mid-fade is composited against whatever is behind it, so contrast is
 * read against a blend that no user ever sees. Infinite animations (the caret,
 * the skeleton shimmer) are skipped — they never settle.
 */
export async function settle(page: Page): Promise<void> {
  await page.evaluate(async () => {
    const finite = document
      .getAnimations()
      .filter((animation) => animation.effect?.getComputedTiming().iterations !== Infinity);
    await Promise.all(finite.map((animation) => animation.finished.catch(() => undefined)));
  });
}

/**
 * One definition of "scanned", shared by every spec that scans. Two copies of
 * `settle` would be two definitions of when a page is ready to measure, free to
 * drift apart, and a contrast result read off the wrong one is wrong quietly.
 */
export async function scan(page: Page) {
  await settle(page);
  return new AxeBuilder({ page }).withTags(TAGS).analyze();
}
