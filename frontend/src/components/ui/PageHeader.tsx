import type { ReactNode } from 'react';

interface PageHeaderProps {
  title: string;
  subtitle?: string;
  action?: ReactNode;
  back?: ReactNode;
}

/**
 * Single page title + supporting description. Used as the visible h1 in the content column.
 */
export default function PageHeader({ title, subtitle, action, back }: PageHeaderProps) {
  return (
    <div className="mb-6">
      {back && <div className="mb-3">{back}</div>}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div className="max-w-2xl">
          <h1 className="font-serif text-[32px] leading-tight text-ink">{title}</h1>
          {subtitle && (
            <p className="page-subtitle mt-2 text-[16px] font-medium leading-6 sm:text-[15px] sm:font-normal">
              {subtitle}
            </p>
          )}
        </div>
        {action && <div className="flex flex-wrap items-center gap-3">{action}</div>}
      </div>
    </div>
  );
}
