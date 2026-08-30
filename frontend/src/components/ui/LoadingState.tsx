interface LoadingStateProps {
  label?: string;
  className?: string;
}

/**
 * Centered spinner with optional label.
 */
export default function LoadingState({ label = 'Loading…', className = '' }: LoadingStateProps) {
  return (
    <div className={`flex flex-col items-center justify-center py-12 ${className}`} role="status">
      <div className="h-8 w-8 animate-spin rounded-full border-2 border-frost border-t-navy" aria-hidden="true" />
      {label && <p className="mt-3 text-sm text-slate">{label}</p>}
    </div>
  );
}
