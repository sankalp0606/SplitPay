import React, { useState } from 'react';
import { Copy, Check, Download, ExternalLink, QrCode } from 'lucide-react';
import { PaymentPart } from '../types/payment';
import { StatusBadge } from './StatusBadge';

interface QrCardProps {
  part: PaymentPart;
  upiId: string;
  recipientName: string;
}

export const QrCard: React.FC<QrCardProps> = ({ part, upiId, recipientName }) => {
  const [copiedUpi, setCopiedUpi] = useState(false);
  const [copiedRef, setCopiedRef] = useState(false);
  const [showQrModal, setShowQrModal] = useState(false);

  const handleCopyUpi = () => {
    navigator.clipboard.writeText(upiId);
    setCopiedUpi(true);
    setTimeout(() => setCopiedUpi(false), 2000);
  };

  const handleCopyRef = () => {
    navigator.clipboard.writeText(part.paymentReference);
    setCopiedRef(true);
    setTimeout(() => setCopiedRef(false), 2000);
  };

  const handleDownloadQr = () => {
    const link = document.createElement('a');
    link.href = part.qrDataUri;
    link.download = `SplitPay_Part_${part.partNumber}_${part.paymentReference}.png`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const isSuccess = part.status === 'SUCCESS';

  return (
    <div
      className={`bg-slate-800/90 border rounded-2xl p-5 shadow-lg flex flex-col justify-between transition-all ${
        isSuccess
          ? 'border-emerald-500/50 ring-1 ring-emerald-500/20'
          : 'border-slate-700/70 hover:border-slate-600'
      }`}
    >
      {/* Header */}
      <div>
        <div className="flex items-center justify-between mb-3">
          <span className="text-xs font-semibold px-2.5 py-1 rounded-md bg-slate-900 border border-slate-700 text-slate-300">
            Payment Part {part.partNumber}
          </span>
          <StatusBadge status={part.status} size="sm" />
        </div>

        <div className="mb-4">
          <span className="text-xs text-slate-400 block">Amount for this part</span>
          <div className="text-2xl font-bold text-white tracking-tight">
            ₹{part.amount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
          </div>
        </div>

        {/* QR Code display */}
        <div className="relative bg-white p-3 rounded-xl flex items-center justify-center mb-4 group shadow-inner mx-auto max-w-[200px]">
          <img
            src={part.qrDataUri}
            alt={`UPI QR Part ${part.partNumber}`}
            className={`w-44 h-44 object-contain ${isSuccess ? 'opacity-40 grayscale' : ''}`}
          />

          {isSuccess && (
            <div className="absolute inset-0 bg-slate-900/60 backdrop-blur-[2px] rounded-xl flex flex-col items-center justify-center text-emerald-400 font-bold text-sm">
              <Check className="w-8 h-8 mb-1 text-emerald-400" />
              <span>Paid &amp; Reconciled</span>
            </div>
          )}
        </div>

        {/* Payment Reference */}
        <div className="bg-slate-900/70 border border-slate-700/50 rounded-lg p-2.5 mb-4 text-xs font-mono text-slate-300 flex items-center justify-between">
          <span className="truncate pr-2" title={part.paymentReference}>
            Ref: {part.paymentReference}
          </span>
          <button
            onClick={handleCopyRef}
            className="text-slate-400 hover:text-white p-1 rounded transition-colors"
            title="Copy reference"
          >
            {copiedRef ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
          </button>
        </div>
      </div>

      {/* Action Buttons */}
      <div className="space-y-2 pt-2 border-t border-slate-700/60 no-print">
        {/* UPI Intent Deep Link (mobile) */}
        <a
          href={part.upiUri}
          className="w-full bg-brand-600 hover:bg-brand-500 text-white font-medium py-2 px-3 rounded-xl text-xs flex items-center justify-center space-x-1.5 transition-colors shadow-sm shadow-brand-600/20"
        >
          <ExternalLink className="w-3.5 h-3.5" />
          <span>Pay via UPI App</span>
        </a>

        <div className="grid grid-cols-2 gap-2">
          <button
            onClick={handleCopyUpi}
            className="w-full bg-slate-700/60 hover:bg-slate-700 text-slate-200 py-1.5 px-2 rounded-xl text-xs flex items-center justify-center space-x-1 transition-colors border border-slate-600/50"
          >
            {copiedUpi ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
            <span>Copy UPI</span>
          </button>

          <button
            onClick={handleDownloadQr}
            className="w-full bg-slate-700/60 hover:bg-slate-700 text-slate-200 py-1.5 px-2 rounded-xl text-xs flex items-center justify-center space-x-1 transition-colors border border-slate-600/50"
          >
            <Download className="w-3.5 h-3.5" />
            <span>Download QR</span>
          </button>
        </div>
      </div>
    </div>
  );
};
