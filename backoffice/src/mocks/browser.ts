import { setupWorker } from 'msw/browser';
import { handlers } from './handlers';

export const worker = setupWorker(...handlers);

/**
 * The mocked API is the app's only backend for now. When the Kotlin service
 * lands, this start-up is what gets dropped — nothing in the components changes.
 */
export async function startMockApi(): Promise<void> {
  await worker.start({
    onUnhandledRequest: 'bypass',
    quiet: true,
    serviceWorker: { url: '/mockServiceWorker.js' },
  });
}
