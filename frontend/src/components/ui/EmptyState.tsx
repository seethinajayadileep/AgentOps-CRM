import type { ReactNode } from 'react';

interface EmptyStateProps {
  icon?: ReactNode;
  title: string;
  description?: string;
  action?: ReactNode;
  className?: string;
}

/**
 * Empty-state block used inside cards and lists.
 */
export default function EmptyState({ icon, title, description, action, className = '' }: EmptyStateProps) {
  return (
    <div className={`px-6 py-12 text-center ${className}`}>
      {icon && (
        <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center border border-frost bg-page text-navy">
          {icon}
        </div>
      )}
      <p className="font-serif text-xl text-ink">{title}</p>
      {description && <p className="mx-auto mt-2 max-w-md text-[15px] leading-6 text-slate">{description}</p>}
      {action && <div className="mt-6 flex justify-center">{action}</div>}
    </div>
  );
}
