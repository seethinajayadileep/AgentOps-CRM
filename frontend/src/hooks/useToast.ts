import { useState, useCallback } from 'react';
import type { ToastData } from '../components/ui/ToastContainer';
import type { ToastType } from '../components/ui/Toast';

let toastId = 0;

export function useToast() {
  const [toasts, setToasts] = useState<ToastData[]>([]);

  const showToast = useCallback((type: ToastType, message: string, duration?: number) => {
    const id = `toast-${++toastId}`;
    const newToast: ToastData = {
      id,
      type,
      message,
      duration: duration ?? (type === 'error' ? 10000 : 5000), // Errors stay longer
    };

    setToasts((prev) => [...prev, newToast]);
  }, []);

  const closeToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((toast) => toast.id !== id));
  }, []);

  return {
    toasts,
    showToast,
    closeToast,
  };
}
