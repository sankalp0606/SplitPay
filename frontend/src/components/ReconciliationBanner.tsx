import React from 'react';
import { StatusBadge } from './StatusBadge';
import { PaymentOrderStatus } from '../types/payment';

interface ReconciliationBannerProps {
  totalAmount: number;
  paidAmount: number;
  remainingAmount: number;
  status: PaymentOrderStatus;
}

export const ReconciliationBanner: React.FC<ReconciliationBannerProps> = ({
  totalAmount,
  paidAmount,
  remainingAmount,
  status,
}) => {
  const percentagePaid = totalAmount > 0 ? Math.min(100, Math.round((paidAmount / totalAmount) * 100)) : 0;

  return (
    <div className="bg-slate-800/80 border border-slate-700/80 rounded-2xl p-6 shadow-xl backdrop-blur-sm">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-5">
        <div>
          <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">Payment Reconciliation Summary</span>
          <h2 className="text-2xl font-bold text-white mt-1">
            ₹{totalAmount.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
          </h2>
        </div>
        <div className="flex items-center space-x-3">
          <StatusBadge status={status} size="lg" />
        </div>
      </div>

      {/* Progress Bar */}
      <div className="w-full bg-slate-950 rounded-full h-3 mb-4 overflow-hidden border border-slate-800">
        <div
          className={`h-full transition-all duration-500 rounded-full ${
            status === 'COMPLETED'
              ? 'bg-emerald-500'
              : percentagePaid > 0
              ? 'bg-gradient-to-r from-brand-500 to-emerald-400'
              : 'bg-slate-700'
          }`}
          style={{ width: `${percentagePaid}%` }}
        />
      </div>

      {/* Metrics Grid */}
      <div className="grid grid-cols-3 gap-3 text-center pt-2 border-t border-slate-700/50">
        <div>
          <span className="text-xs text-slate-400 block mb-0.5">Total Expected</span>
          <span className="text-sm sm:text-base font-semibold text-white">
            ₹{totalAmount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
          </span>
        </div>
        <div>
          <span className="text-xs text-slate-400 block mb-0.5">Total Paid</span>
          <span className="text-sm sm:text-base font-semibold text-emerald-400">
            ₹{paidAmount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
          </span>
        </div>
        <div>
          <span className="text-xs text-slate-400 block mb-0.5">Remaining Balance</span>
          <span className="text-sm sm:text-base font-semibold text-amber-400">
            ₹{remainingAmount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
          </span>
        </div>
      </div>
    </div>
  );
};
