import Badge from '../ui/Badge';
import { Channel } from '../../types/conversation';
import { MessageCircle, Mail, Phone, MessageSquare, Send } from 'lucide-react';

interface ChannelBadgeProps {
  channel: Channel;
}

/**
 * Badge component for conversation channel display.
 *
 * @version 0.3.0
 * Feature: F-009 - Conversations Admin Page
 */
export default function ChannelBadge({ channel }: ChannelBadgeProps) {
  const channelConfig: Record<Channel, { label: string; color: 'purple' | 'blue' | 'cyan' | 'green'; icon: any }> = {
    [Channel.WEB_WIDGET]: { label: 'Web Widget', color: 'purple', icon: MessageCircle },
    [Channel.EMAIL]: { label: 'Email', color: 'blue', icon: Mail },
    [Channel.PHONE]: { label: 'Phone', color: 'cyan', icon: Phone },
    [Channel.SMS]: { label: 'SMS', color: 'green', icon: MessageSquare },
    [Channel.WHATSAPP]: { label: 'WhatsApp', color: 'green', icon: Send },
  };

  const config = channelConfig[channel] || { label: channel, color: 'purple' as const, icon: MessageCircle };
  const Icon = config.icon;

  return (
    <div className="flex items-center gap-1.5">
      <Icon size={14} />
      <Badge color={config.color}>{config.label}</Badge>
    </div>
  );
}
