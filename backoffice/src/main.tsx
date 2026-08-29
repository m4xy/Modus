import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './App';
import './styles/global.css';

/**
 * The mocked API starts before the first render so no component ever races a
 * half-installed service worker. It is dynamically imported behind a flag, so
 * pointing the app at a real backend both switches the data source and drops
 * MSW out of the bundle entirely.
 */
async function bootstrap() {
    if (import.meta.env['VITE_MOCK_API'] !== 'false') {
        const { startMockApi } = await import('./mocks/browser');
        await startMockApi();
    }

    const container = document.getElementById('root');
    if (!container) throw new Error('Missing #root element.');

    createRoot(container).render(
        <StrictMode>
            <App />
        </StrictMode>,
    );
}

void bootstrap();
