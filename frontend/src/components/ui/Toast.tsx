import { useEffect } from 'react';
import { X, CheckCircle2, AlertCircle, Info } from 'lucide-react';

export type ToastType = 'success' | 'error' | 'info';

export interface ToastProps {
  id: string;
  type: ToastType;
  message: string;
  duration?: number;
  onClose: (id: string) => void;
}

/**
  * Toast notification component with auto-dismiss and manual close.
  * Accessible with ARIA live regions.
  */
export default function Toast({ id, type, message, duration = 5000, onClose }: ToastProps) {
  useEffect(() => {
    if (type === 'success' && duration > 0) {
      const timer = setTimeout(() => {
        onClose(id);
      }, duration);
      return () => clearTimeout(timer);
    }
  }, [id, type, duration, onClose]);

  const getIcon = () => {
    switch (type) {
      case 'success':
        return <CheckCircle2 size={20} className="text-ink" />;
      case 'error':
        return <AlertCircle size={20} className="text-ink" />;
      case 'info':
        return <Info size={20} className="text-slate" />;
    }
  };

  return (
    <div
      role="alert"
      aria-live={type === 'error' ? 'assertive' : 'polite'}
      aria-atomic="true"
      className="pointer-events-auto flex w-full max-w-md items-start gap-3 border border-frost bg-snow p-4 shadow-classic transition-all"
    >
      <div className="flex-shrink-0 pt-0.5">{getIcon()}</div>
      <div className="flex-1">
        <p className="text-sm text-ink">{message}</p>
      </div>
      <button
        onClick={() => onClose(id)}
        className="flex-shrink-0 p-1 text-silver transition-colors hover:bg-mist hover:text-ink"
        aria-label="Close notification"
      >
        <X size={16} />
      </button>
    </div>
  );
}
