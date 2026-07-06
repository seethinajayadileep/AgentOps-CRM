import Badge, { type BadgeColor } from './Badge';

/**
 * Maps a status string to a themed badge color following the app's badge rules:
 * - NEW: blue
 * - QUALIFIED / APPROVED / COMPLETED / IMPORTED / ANSWERED / SUCCESS: green
 * - HOT / PENDING / REVIEWED / NO_ANSWER / BUSY / VOICEMAIL: amber
 * - IN_PROGRESS / RUNNING / STARTED: cyan
 * - COLD / FAILED / REJECTED / CANCELLED / BLOCKED: red / muted
 */
export function statusColor(status: string): BadgeColor {
  const s = (status || '').toUpperCase();
  switch (s) {
    case 'NEW':
      return 'blue';
    case 'QUALIFIED':
    case 'APPROVED':
    case 'COMPLETED':
    case 'IMPORTED':
    case 'ANSWERED':
    case 'SUCCESS':
      return 'green';
    case 'HOT':
    case 'PENDING':
    case 'REVIEWED':
    case 'NO_ANSWER':
    case 'BUSY':
    case 'VOICEMAIL':
      return 'amber';
    case 'IN_PROGRESS':
    case 'RUNNING':
    case 'STARTED':
      return 'cyan';
    case 'FOLLOWED_UP':
      return 'purple';
    case 'FAILED':
    case 'REJECTED':
    case 'COLD':
    case 'BLOCKED':
      return 'red';
    case 'CANCELLED':
    case 'CLOSED':
    case 'NOT_STARTED':
      return 'gray';
    default:
      return 'gray';
  }
}

interface StatusBadgeProps {
  status: string;
  className?: string;
}

/**
 * Status pill that auto-colors based on the status value and prettifies the label.
 */
export default function StatusBadge({ status, className = '' }: StatusBadgeProps) {
  const label = (status || '').replace(/_/g, ' ');
  return (
    <Badge color={statusColor(status)} className={className}>
      {label}
    </Badge>
  );
}
