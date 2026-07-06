import Badge, { type BadgeColor } from '../ui/Badge';
import { ReadinessStatus } from '../../types/settings';

interface ReadinessBadgeProps {
  status: ReadinessStatus;
  className?: string;
}

/**
 * Badge component for displaying ReadinessStatus with appropriate color.
 *
 * @version 0.1.0
 * Feature: Settings Page Implementation
 */
export default function ReadinessBadge({ status, className = '' }: ReadinessBadgeProps) {
  const getColor = (status: ReadinessStatus): BadgeColor => {
    switch (status) {
      case ReadinessStatus.HEALTHY:
        return 'green';
      case ReadinessStatus.CONFIGURED:
        return 'blue';
      case ReadinessStatus.NOT_CONFIGURED:
        return 'gray';
      case ReadinessStatus.DISABLED:
        return 'gray';
      case ReadinessStatus.DEGRADED:
        return 'amber';
      case ReadinessStatus.ERROR:
        return 'red';
      case ReadinessStatus.UNKNOWN:
      default:
        return 'gray';
    }
  };

  const getLabel = (status: ReadinessStatus): string => {
    return status.replace(/_/g, ' ');
  };

  return (
    <Badge color={getColor(status)} className={className}>
      {getLabel(status)}
    </Badge>
  );
}
