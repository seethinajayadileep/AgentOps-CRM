import type { ReactNode } from 'react';

interface CardProps {
  children: ReactNode;
  className?: string;
  hover?: boolean;
  onClick?: () => void;
}

/**
 * Glassmorphism surface card (dark-mode-first).
 */
export default function Card({ children, className = '', hover = false, onClick }: CardProps) {
  return (
    <div
      onClick={onClick}
      className={`glass-card ${hover ? 'glass-card-hover cursor-pointer' : ''} ${className}`}
    >
      {children}
    </div>
  );
}
