import type { ReactNode } from 'react';

export type BadgeColor = 'purple' | 'blue' | 'cyan' | 'green' | 'amber' | 'red' | 'gray' | 'gold';

const colorClass: Record<BadgeColor, string> = {
  purple: 'bg-pale-navy text-navy border-navy/20',
  blue: 'bg-pale-navy text-navy border-navy/20',
  cyan: 'bg-pale-success text-success border-success/25',
  green: 'bg-pale-success text-success border-success/25',
  amber: 'bg-pale-warning text-warning border-warning/25',
  red: 'bg-pale-error text-error border-error/25',
  gray: 'bg-page text-slate border-frost',
  gold: 'bg-pale-gold text-gold border-gold/30',
};

interface BadgeProps {
  color?: BadgeColor;
  children: ReactNode;
  className?: string;
}

/**
 * Status badge using semantic colors plus a text label (never color alone).
 */
export default function Badge({ color = 'gray', children, className = '' }: BadgeProps) {
  return <span className={`badge ${colorClass[color]} ${className}`}>{children}</span>;
}
