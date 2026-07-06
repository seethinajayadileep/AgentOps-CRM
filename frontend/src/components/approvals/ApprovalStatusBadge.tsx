import React from 'react';
import { ApprovalStatus } from '../../types/approval';
import Badge, { type BadgeColor } from '../ui/Badge';

interface ApprovalStatusBadgeProps {
  status: ApprovalStatus;
}

const ApprovalStatusBadge: React.FC<ApprovalStatusBadgeProps> = ({ status }) => {
  const getColor = (): BadgeColor => {
    switch (status) {
      case ApprovalStatus.PENDING:
        return 'amber';
      case ApprovalStatus.APPROVED:
        return 'green';
      case ApprovalStatus.REJECTED:
        return 'red';
      default:
        return 'gray';
    }
  };

  return <Badge color={getColor()}>{status}</Badge>;
};

export default ApprovalStatusBadge;
