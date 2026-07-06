import Toast, { ToastType } from './Toast';

export interface ToastData {
  id: string;
  type: ToastType;
  message: string;
  duration?: number;
}

interface ToastContainerProps {
  toasts: ToastData[];
  onClose: (id: string) => void;
}

/**
 * Container that positions toasts in the top-right corner.
 */
export default function ToastContainer({ toasts, onClose }: ToastContainerProps) {
  return (
    <div
      className="pointer-events-none fixed right-0 top-0 z-50 flex max-h-screen w-full flex-col items-end gap-3 overflow-hidden p-4 sm:max-w-md sm:p-6"
      aria-live="polite"
      aria-label="Notifications"
    >
      {toasts.map((toast) => (
        <Toast key={toast.id} {...toast} onClose={onClose} />
      ))}
    </div>
  );
}
