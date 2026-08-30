interface ErrorBannerProps {
  message: string;
  onRetry?: () => void;
  onClear?: () => void;
}

export default function ErrorBanner({ message, onRetry, onClear }: ErrorBannerProps) {
  return (
    <div className="mb-6 rounded-sm border border-frost bg-mist p-4 text-ink" role="alert">
      <p>{message}</p>
      <div className="mt-3 flex flex-wrap gap-3">
        {onRetry && (
          <button type="button" onClick={onRetry} className="btn-secondary">
            Retry
          </button>
        )}
        {onClear && (
          <button type="button" onClick={onClear} className="btn-secondary">
            Clear filters
          </button>
        )}
      </div>
    </div>
  );
}
