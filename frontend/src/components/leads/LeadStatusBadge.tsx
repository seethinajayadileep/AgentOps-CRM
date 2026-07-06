import React from 'react';
import { LeadStatus } from '../../types/lead';
import Badge, { type BadgeColor } from '../ui/Badge';

interface LeadStatusBadgeProps {
  status: LeadStatus;
}

export const LeadStatusBadge: React.FC<LeadStatusBadgeProps> = ({ status }) => {
  const getColor = (s: LeadStatus): BadgeColor => {
    switch (s) {
      case LeadStatus.HOT:
        return 'amber';
      case LeadStatus.QUALIFIED:
        return 'green';
      case LeadStatus.NEW:
        return 'blue';
      case LeadStatus.COLD:
        return 'red';
      case LeadStatus.FOLLOWED_UP:
        return 'purple';
      case LeadStatus.CLOSED:
        return 'gray';
      default:
        return 'gray';
    }
  };

  return <Badge color={getColor(status)}>{status}</Badge>;
};
