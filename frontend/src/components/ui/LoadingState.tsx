interface LoadingStateProps {
  label?: string;
  className?: string;
}

/**
 * Centered spinner with optional label.
 */
export default function LoadingState({ label = 'Loading…', className = '' }: LoadingStateProps) {
  return (
    <div className={`flex flex-col items-center justify-center py-12 ${className}`}>
      <div className="h-8 w-8 animate-spin rounded-full border-2 border-white/10 border-t-primary-500" />
      {label && <p className="mt-3 text-sm text-zinc-400">{label}</p>}
    </div>
  );
}
