import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router';
import { DomainRoute } from './app/DomainRoute';
import { RootRedirect } from './app/RootRedirect';
import { ThemeProvider } from './app/ThemeProvider';
import { RequireCapability } from './components/RequireCapability';
import { AgentConsole } from './routes/AgentConsole';
import { Cost } from './routes/Cost';
import { Memories } from './routes/Memories';
import { NotFound } from './routes/NotFound';
import { Repositories } from './routes/Repositories';
import { Settings } from './routes/Settings';
import { Skills } from './routes/Skills';
import { Work } from './routes/Work';
import { ToastProvider } from './ui';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
      staleTime: 30_000,
    },
  },
});

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>
        <ToastProvider>
          <BrowserRouter>
            <Routes>
              <Route path="/" element={<RootRedirect />} />
              {/* Every product surface hangs off the tenant root. */}
              <Route path="/domains/:domainId" element={<DomainRoute />}>
                <Route index element={<Navigate to="work" replace />} />
                <Route
                  path="work"
                  element={
                    <RequireCapability capability="work.read">
                      <Work />
                    </RequireCapability>
                  }
                />
                <Route
                  path="repositories"
                  element={
                    <RequireCapability capability="repositories.read">
                      <Repositories />
                    </RequireCapability>
                  }
                />
                <Route
                  path="agents"
                  element={
                    <RequireCapability capability="agents.read">
                      <AgentConsole />
                    </RequireCapability>
                  }
                />
                <Route
                  path="memories"
                  element={
                    <RequireCapability capability="memories.read">
                      <Memories />
                    </RequireCapability>
                  }
                />
                <Route
                  path="cost"
                  element={
                    <RequireCapability capability="cost.read">
                      <Cost />
                    </RequireCapability>
                  }
                />
                <Route
                  path="skills"
                  element={
                    <RequireCapability capability="skills.read">
                      <Skills />
                    </RequireCapability>
                  }
                />
                <Route
                  path="settings"
                  element={
                    <RequireCapability capability="settings.read">
                      <Settings />
                    </RequireCapability>
                  }
                />
              </Route>
              <Route path="*" element={<NotFound />} />
            </Routes>
          </BrowserRouter>
        </ToastProvider>
      </ThemeProvider>
    </QueryClientProvider>
  );
}
