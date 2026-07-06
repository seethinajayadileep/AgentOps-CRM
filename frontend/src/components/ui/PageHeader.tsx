import type { ReactNode } from 'react';

interface PageHeaderProps {
  title: string;
  subtitle?: string;
  action?: ReactNode;
  back?: ReactNode;
}

/**
 * Consistent page header with title, subtitle, optional back link and main action.
 */
export default function PageHeader({ title, subtitle, action, back }: PageHeaderProps) {
  return (
    <div className="mb-6">
      {back && <div className="mb-3">{back}</div>}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">{title}</h1>
          {subtitle && <p className="mt-1 text-sm text-zinc-400">{subtitle}</p>}
        </div>
        {action && <div className="flex flex-wrap items-center gap-3">{action}</div>}
      </div>
    </div>
  );
}
