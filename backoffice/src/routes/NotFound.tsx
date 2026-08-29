import { Link } from 'react-router';
import { EmptyState } from '../ui';

export function NotFound() {
  return (
    <div style={{ padding: 'var(--space-16) var(--space-6)' }}>
      <EmptyState
        title="That page does not exist"
        description="Every screen in Modus lives under a domain. Start from your first domain and navigate from there."
        action={<Link to="/">Go to your domains</Link>}
      />
    </div>
  );
}
