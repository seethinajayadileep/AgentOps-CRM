import type { ReactNode } from 'react';

export type BadgeColor = 'purple' | 'blue' | 'cyan' | 'green' | 'amber' | 'red' | 'gray';

const colorClass: Record<BadgeColor, string> = {
  purple: 'bg-primary-500/15 text-primary-300 border-primary-500/30',
  blue: 'bg-blue-500/15 text-blue-300 border-blue-500/30',
  cyan: 'bg-cyan-500/15 text-cyan-300 border-cyan-500/30',
  green: 'bg-green-500/15 text-green-300 border-green-500/30',
  amber: 'bg-amber-500/15 text-amber-300 border-amber-500/30',
  red: 'bg-red-500/15 text-red-300 border-red-500/30',
  gray: 'bg-white/5 text-zinc-300 border-white/10',
};

interface BadgeProps {
  color?: BadgeColor;
  children: ReactNode;
  className?: string;
}

/**
 * Generic pill badge with themed colors.
 */
export default function Badge({ color = 'gray', children, className = '' }: BadgeProps) {
  return <span className={`badge ${colorClass[color]} ${className}`}>{children}</span>;
}
