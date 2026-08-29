import { fileURLToPath } from 'node:url';
import path from 'node:path';
import { defineConfig, devices } from '@playwright/test';

const here = path.dirname(fileURLToPath(import.meta.url));
const backoffice = path.resolve(here, '../backoffice');

const PORT = 4173;
const BASE_URL = `http://127.0.0.1:${PORT}`;

/**
 * Tests run against a production build of the backoffice with the mocked API
 * still installed, so the suite exercises the same bundle a reviewer would open
 * — not a dev server with different behaviour.
 */
export default defineConfig({
    testDir: path.join(here, 'tests'),
    fullyParallel: true,
    forbidOnly: Boolean(process.env['CI']),
    retries: process.env['CI'] ? 2 : 0,
    ...(process.env['CI'] ? { workers: 2 } : {}),
    reporter: process.env['CI'] ? [['github'], ['html', { open: 'never' }]] : [['list']],
    timeout: 30_000,
    expect: { timeout: 7_000 },

    use: {
        baseURL: BASE_URL,
        trace: 'on-first-retry',
        screenshot: 'only-on-failure',
        // Deterministic viewport keeps the rail visible: below 60rem it collapses.
        viewport: { width: 1440, height: 900 },
    },

    projects: [
        {
            name: 'chromium',
            use: { ...devices['Desktop Chrome'] },
        },
    ],

    webServer: {
        command: 'npm run build && npm run preview',
        cwd: backoffice,
        url: BASE_URL,
        timeout: 120_000,
        reuseExistingServer: !process.env['CI'],
        stdout: 'ignore',
        stderr: 'pipe',
    },
});
