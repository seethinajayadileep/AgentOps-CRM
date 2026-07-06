import { ConversationStatus } from '../../types/conversation';
import StatusBadge from '../ui/StatusBadge';

interface ConversationStatusBadgeProps {
  status: ConversationStatus;
}

/**
 * Badge component for conversation status display.
 *
 * @version 0.3.0
 * Feature: F-009 - Conversations Admin Page
 */
export default function ConversationStatusBadge({ status }: ConversationStatusBadgeProps) {
  return <StatusBadge status={status} />;
}
