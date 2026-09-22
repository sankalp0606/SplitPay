import React from 'react';
import { CheckCircle2, Clock, XCircle, AlertCircle, RefreshCw } from 'lucide-react';
import { PaymentOrderStatus, PaymentPartStatus } from '../types/payment';

interface StatusBadgeProps {
  status: PaymentOrderStatus | PaymentPartStatus;
  size?: 'sm' | 'md' | 'lg';
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status, size = 'md' }) => {
  const getBadgeConfig = () => {
    switch (status) {
      case 'SUCCESS':
      case 'COMPLETED':
        return {
          bg: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30',
          icon: <CheckCircle2 className="w-3.5 h-3.5 mr-1" />,
          label: status === 'COMPLETED' ? 'COMPLETED' : 'SUCCESS',
        };
      case 'PARTIALLY_PAID':
        return {
          bg: 'bg-amber-500/10 text-amber-400 border-amber-500/30',
          icon: <RefreshCw className="w-3.5 h-3.5 mr-1 animate-spin" />,
          label: 'PARTIALLY PAID',
        };
      case 'PENDING':
        return {
          bg: 'bg-blue-500/10 text-blue-400 border-blue-500/30',
          icon: <Clock className="w-3.5 h-3.5 mr-1" />,
          label: 'PENDING',
        };
      case 'FAILED':
        return {
          bg: 'bg-rose-500/10 text-rose-400 border-rose-500/30',
          icon: <XCircle className="w-3.5 h-3.5 mr-1" />,
          label: 'FAILED',
        };
      case 'EXPIRED':
      case 'CANCELLED':
        return {
          bg: 'bg-slate-500/10 text-slate-400 border-slate-500/30',
          icon: <AlertCircle className="w-3.5 h-3.5 mr-1" />,
          label: status,
        };
      default:
        return {
          bg: 'bg-slate-500/10 text-slate-400 border-slate-500/30',
          icon: null,
          label: status,
        };
    }
  };

  const config = getBadgeConfig();
  const sizeClasses = size === 'sm' ? 'text-xs px-2 py-0.5' : size === 'lg' ? 'text-sm px-3 py-1.5' : 'text-xs px-2.5 py-1';

  return (
    <span
      className={`inline-flex items-center font-medium rounded-full border ${config.bg} ${sizeClasses}`}
      role="status"
      aria-label={`Status: ${config.label}`}
    >
      {config.icon}
      {config.label}
    </span>
  );
};
