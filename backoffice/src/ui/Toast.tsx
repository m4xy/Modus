import { createContext, useCallback, useContext, useMemo, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import { Button } from './Button';
import { cx } from './cx';
import styles from './Toast.module.css';

export type ToastTone = 'info' | 'success' | 'warning' | 'error';

export interface Toast {
    id: number;
    tone: ToastTone;
    title: string;
    body?: string;
}

interface ToastContextValue {
    notify: (toast: Omit<Toast, 'id'>) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

export function useToast(): ToastContextValue {
    const context = useContext(ToastContext);
    if (!context) throw new Error('useToast must be used inside <ToastProvider>.');
    return context;
}

const DISMISS_AFTER_MS = 6000;

export function ToastProvider({ children }: { children: ReactNode }) {
    const [toasts, setToasts] = useState<Toast[]>([]);
    const nextId = useRef(1);

    const dismiss = useCallback((id: number) => {
        setToasts((current) => current.filter((toast) => toast.id !== id));
    }, []);

    const notify = useCallback(
        (toast: Omit<Toast, 'id'>) => {
            const id = nextId.current++;
            setToasts((current) => [...current, { ...toast, id }]);
            window.setTimeout(() => dismiss(id), DISMISS_AFTER_MS);
        },
        [dismiss],
    );

    const value = useMemo(() => ({ notify }), [notify]);

    return (
        <ToastContext.Provider value={value}>
            {children}
            {/*
        A polite live region: announcements queue behind whatever the user is
        doing instead of interrupting them mid-sentence.
      */}
            <div
                className={styles.region}
                role="status"
                aria-live="polite"
                aria-label="Notifications"
            >
                {toasts.map((toast) => (
                    <div key={toast.id} className={cx(styles.toast, styles[toast.tone])}>
                        <div className={styles.content}>
                            <p className={styles.title}>{toast.title}</p>
                            {toast.body && <p className={styles.body}>{toast.body}</p>}
                        </div>
                        <Button
                            variant="ghost"
                            size="sm"
                            iconOnly
                            aria-label={`Dismiss: ${toast.title}`}
                            onClick={() => dismiss(toast.id)}
                        >
                            <svg width="12" height="12" viewBox="0 0 14 14" aria-hidden="true">
                                <path
                                    d="M2 2l10 10M12 2L2 12"
                                    stroke="currentColor"
                                    strokeWidth="1.5"
                                    strokeLinecap="round"
                                />
                            </svg>
                        </Button>
                    </div>
                ))}
            </div>
        </ToastContext.Provider>
    );
}
