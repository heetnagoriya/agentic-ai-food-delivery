import { useState, useCallback, useRef, useEffect } from 'react';

let toastIdCounter = 0;

export function useToast() {
    const [toasts, setToasts] = useState([]);
    const timersRef = useRef({});

    const removeToast = useCallback((id) => {
        setToasts((prev) => prev.filter((t) => t.id !== id));
        if (timersRef.current[id]) {
            clearTimeout(timersRef.current[id]);
            delete timersRef.current[id];
        }
    }, []);

    const addToast = useCallback((message, type = 'error', duration = 5000) => {
        const id = ++toastIdCounter;
        setToasts((prev) => [...prev, { id, message, type }]);
        timersRef.current[id] = setTimeout(() => removeToast(id), duration);
        return id;
    }, [removeToast]);

    useEffect(() => {
        return () => {
            Object.values(timersRef.current).forEach(clearTimeout);
        };
    }, []);

    return { toasts, addToast, removeToast };
}

export default function ToastContainer({ toasts, removeToast }) {
    return (
        <div className="toast-container" id="toast-container">
            {toasts.map((toast) => (
                <div
                    key={toast.id}
                    className={`toast toast-${toast.type}`}
                    id={`toast-${toast.id}`}
                    onClick={() => removeToast(toast.id)}
                >
                    <span className="toast-icon">
                        {toast.type === 'error' ? '❌' : toast.type === 'success' ? '✅' : 'ℹ️'}
                    </span>
                    <span className="toast-message">{toast.message}</span>
                    <button className="toast-close" aria-label="Dismiss">×</button>
                </div>
            ))}
        </div>
    );
}
