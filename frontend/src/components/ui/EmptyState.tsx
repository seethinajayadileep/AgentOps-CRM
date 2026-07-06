import type { ReactNode } from 'react';

interface EmptyStateProps {
  icon?: ReactNode;
  title: string;
  description?: string;
  action?: ReactNode;
  className?: string;
}

/**
 * Friendly empty-state block used inside cards/lists.
 */
export default function EmptyState({ icon, title, description, action, className = '' }: EmptyStateProps) {
  return (
    <div className={`glass-card p-12 text-center ${className}`}>
      {icon && (
        <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-white/5 text-zinc-400">
          {icon}
        </div>
      )}
      <p className="font-medium text-zinc-200">{title}</p>
      {description && <p className="mt-1 text-sm text-zinc-500">{description}</p>}
      {action && <div className="mt-5 flex justify-center">{action}</div>}
    </div>
  );
}
