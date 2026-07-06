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
        return <CheckCircle2 size={20} className="text-green-400" />;
      case 'error':
        return <AlertCircle size={20} className="text-red-400" />;
      case 'info':
        return <Info size={20} className="text-blue-400" />;
    }
  };

  const getStyles = () => {
    switch (type) {
      case 'success':
        return 'border-green-500/30 bg-green-500/10';
      case 'error':
        return 'border-red-500/30 bg-red-500/10';
      case 'info':
        return 'border-blue-500/30 bg-blue-500/10';
    }
  };

  const getTextColor = () => {
    switch (type) {
      case 'success':
        return 'text-green-100';
      case 'error':
        return 'text-red-100';
      case 'info':
        return 'text-blue-100';
    }
  };

  return (
    <div
      role="alert"
      aria-live={type === 'error' ? 'assertive' : 'polite'}
      aria-atomic="true"
      className={`pointer-events-auto flex w-full max-w-md items-start gap-3 rounded-xl border p-4 shadow-lg backdrop-blur-sm transition-all ${getStyles()}`}
    >
      <div className="flex-shrink-0 pt-0.5">{getIcon()}</div>
      <div className="flex-1">
        <p className={`text-sm ${getTextColor()}`}>{message}</p>
      </div>
      <button
        onClick={() => onClose(id)}
        className="flex-shrink-0 rounded-lg p-1 text-zinc-400 transition-colors hover:bg-white/10 hover:text-zinc-100"
        aria-label="Close notification"
      >
        <X size={16} />
      </button>
    </div>
  );
}
