import React from 'react';
import Badge, { type BadgeColor } from '../ui/Badge';

interface LeadScoreBadgeProps {
  score?: number;
}

export const LeadScoreBadge: React.FC<LeadScoreBadgeProps> = ({ score }) => {
  if (score === undefined || score === null) {
    return <span className="text-zinc-500">-</span>;
  }

  const getColor = (s: number): BadgeColor => {
    if (s >= 75) return 'green';
    if (s >= 50) return 'cyan';
    if (s >= 25) return 'amber';
    return 'gray';
  };

  return <Badge color={getColor(score)}>{score.toFixed(0)}</Badge>;
};
