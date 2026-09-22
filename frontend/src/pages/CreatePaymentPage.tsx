import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { QrCode, ArrowRight, AlertCircle, Sparkles, SlidersHorizontal, Info } from 'lucide-react';
import { paymentService } from '../services/paymentService';
import { useAuth } from '../context/AuthContext';
import { SplittingStrategyType } from '../types/payment';

export const CreatePaymentPage: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  // Essential Core Fields
  const [recipientName, setRecipientName] = useState(user?.businessName || user?.fullName || '');
  const [upiId, setUpiId] = useState(user?.defaultUpiId || '');
  const [amountStr, setAmountStr] = useState('5000');

  // Splitting Engine Settings
  const [showAdvanced, setShowAdvanced] = useState(false);
  const [strategy, setStrategy] = useState<SplittingStrategyType>('MAX_PART_AMOUNT');
  const [maxPartAmountStr, setMaxPartAmountStr] = useState('1990.00');

  // Real-time Preview State
  const [previewParts, setPreviewParts] = useState<number[]>([]);
  const [previewLoading, setPreviewLoading] = useState(false);

  // Form State
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  // Fetch Live Split Preview
  useEffect(() => {
    const numAmount = parseFloat(amountStr);
    const numMax = parseFloat(maxPartAmountStr);

    if (numAmount && numAmount > 0) {
      setPreviewLoading(true);
      const timer = setTimeout(async () => {
        try {
          const res = await paymentService.previewPlan({
            totalAmount: numAmount,
            splittingStrategy: strategy,
            maxPartAmount: numMax && numMax > 0 ? numMax : 1990,
          });
          setPreviewParts(res.parts);
        } catch {
          // Fallback client preview calculation
          const maxPart = numMax && numMax > 0 ? numMax : 1990;
          const parts: number[] = [];
          let rem = numAmount;
          while (rem > maxPart) {
            parts.push(maxPart);
            rem = Number((rem - maxPart).toFixed(2));
          }
          if (rem > 0) parts.push(rem);
          setPreviewParts(parts);
        } finally {
          setPreviewLoading(false);
        }
      }, 250);

      return () => clearTimeout(timer);
    } else {
      setPreviewParts([]);
    }
  }, [amountStr, strategy, maxPartAmountStr]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    const totalAmount = parseFloat(amountStr);
    if (isNaN(totalAmount) || totalAmount < 1) {
      setError('Amount must be at least ₹1.00');
      return;
    }

    setSubmitting(true);
    try {
      const maxPart = parseFloat(maxPartAmountStr);
      const order = await paymentService.createPaymentOrder({
        recipientName: recipientName.trim(),
        upiId: upiId.trim(),
        totalAmount,
        splittingStrategy: strategy,
        maxPartAmount: maxPart && maxPart > 0 ? maxPart : undefined,
      });

      navigate(`/payment/${order.id}`);
    } catch (err: any) {
      const msg = err.response?.data?.message ||
        err.response?.data?.errors?.upiId ||
        err.response?.data?.errors?.totalAmount ||
        'Failed to create payment order. Please verify input fields.';
      setError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <div className="text-center mb-8">
        <h1 className="text-3xl font-extrabold text-white tracking-tight">Create Payment</h1>
        <p className="text-slate-400 text-sm mt-1.5">
          Enter recipient details and amount. SplitPay generates individual UPI QR requests for each split part.
        </p>
      </div>

      <div className="bg-slate-800/90 border border-slate-700/80 rounded-3xl p-6 sm:p-8 shadow-2xl backdrop-blur-xl">
        {error && (
          <div className="mb-6 bg-rose-500/10 border border-rose-500/30 rounded-xl p-4 flex items-start space-x-2.5 text-rose-400 text-xs">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Essential Field 1: Recipient Name */}
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-2" htmlFor="recipient-name">
              1. Recipient / Merchant Name
            </label>
            <input
              id="recipient-name"
              type="text"
              required
              value={recipientName}
              onChange={(e) => setRecipientName(e.target.value)}
              placeholder="e.g. ABC Electronics"
              className="w-full px-4 py-3 bg-slate-900/90 border border-slate-700 rounded-xl text-white text-base focus:outline-none focus:border-brand-500 transition-colors"
            />
          </div>

          {/* Essential Field 2: UPI ID */}
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-2" htmlFor="upi-id">
              2. Recipient Payee UPI ID
            </label>
            <input
              id="upi-id"
              type="text"
              required
              value={upiId}
              onChange={(e) => setUpiId(e.target.value)}
              placeholder="e.g. abcelectronics@upi"
              className="w-full px-4 py-3 bg-slate-900/90 border border-slate-700 rounded-xl text-white text-base font-mono focus:outline-none focus:border-brand-500 transition-colors"
            />
            <span className="text-[11px] text-slate-500 mt-1 block">
              Format: handle@bank (e.g. abcelectronics@upi, merchant@okaxis, store@icici)
            </span>
          </div>

          {/* Essential Field 3: Total Amount */}
          <div>
            <div className="flex items-center justify-between mb-2">
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300" htmlFor="total-amount">
                3. Total Amount (₹)
              </label>
              {/* Quick Select Buttons */}
              <div className="flex items-center space-x-1.5">
                {['2000', '5000', '7500', '10000'].map((amt) => (
                  <button
                    key={amt}
                    type="button"
                    onClick={() => setAmountStr(amt)}
                    className="text-[10px] px-2 py-0.5 rounded-md bg-slate-900 border border-slate-700 text-slate-400 hover:text-white transition-colors"
                  >
                    ₹{parseInt(amt).toLocaleString('en-IN')}
                  </button>
                ))}
              </div>
            </div>

            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none text-slate-400 font-bold text-lg">
                ₹
              </div>
              <input
                id="total-amount"
                type="number"
                step="0.01"
                min="1.00"
                required
                value={amountStr}
                onChange={(e) => setAmountStr(e.target.value)}
                placeholder="5000"
                className="w-full pl-9 pr-4 py-3 bg-slate-900/90 border border-slate-700 rounded-xl text-white text-xl font-bold focus:outline-none focus:border-brand-500 transition-colors"
              />
            </div>
          </div>

          {/* Live Splitting Calculation Preview Banner */}
          {previewParts.length > 0 && (
            <div className="bg-slate-950/80 border border-slate-700/60 rounded-2xl p-4 sm:p-5">
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center space-x-2 text-xs font-semibold text-brand-300">
                  <Sparkles className="w-3.5 h-3.5 text-brand-400" />
                  <span>Calculated Payment Split Plan ({previewParts.length} Parts)</span>
                </div>
                <span className="text-xs text-slate-400">
                  Max Part: ₹{parseFloat(maxPartAmountStr || '1990').toLocaleString('en-IN')}
                </span>
              </div>

              <div className="grid grid-cols-2 sm:grid-cols-3 gap-2.5">
                {previewParts.map((part, index) => (
                  <div key={index} className="bg-slate-900/90 border border-slate-800 rounded-xl p-3 text-center">
                    <span className="text-[10px] font-semibold text-slate-400 uppercase block">Part {index + 1}</span>
                    <span className="text-base font-bold text-white">
                      ₹{part.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Advanced Splitting Options Toggle */}
          <div className="pt-2">
            <button
              type="button"
              onClick={() => setShowAdvanced(!showAdvanced)}
              className="text-xs text-slate-400 hover:text-slate-200 flex items-center space-x-1.5 transition-colors"
            >
              <SlidersHorizontal className="w-3.5 h-3.5" />
              <span>{showAdvanced ? 'Hide Splitting Settings' : 'Configure Splitting Strategy'}</span>
            </button>

            {showAdvanced && (
              <div className="mt-3 p-4 bg-slate-900/70 border border-slate-800 rounded-xl space-y-3">
                <div>
                  <label className="block text-xs text-slate-400 mb-1" htmlFor="split-strategy">
                    Splitting Strategy
                  </label>
                  <select
                    id="split-strategy"
                    value={strategy}
                    onChange={(e) => setStrategy(e.target.value as SplittingStrategyType)}
                    className="w-full px-3 py-2 bg-slate-950 border border-slate-700 rounded-lg text-white text-xs"
                  >
                    <option value="MAX_PART_AMOUNT">Maximum Part Amount (Default ₹1,990)</option>
                    <option value="EQUAL_SPLIT">Equal Proportional Split</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs text-slate-400 mb-1" htmlFor="max-part-amount">
                    Configurable Max Part Amount (₹)
                  </label>
                  <input
                    id="max-part-amount"
                    type="number"
                    step="0.01"
                    min="1.00"
                    value={maxPartAmountStr}
                    onChange={(e) => setMaxPartAmountStr(e.target.value)}
                    className="w-full px-3 py-2 bg-slate-950 border border-slate-700 rounded-lg text-white text-xs font-mono"
                  />
                  <span className="text-[11px] text-slate-500 mt-1 block">
                    Default is ₹1,990.00 to safely bypass banking single-transaction flags.
                  </span>
                </div>
              </div>
            )}
          </div>

          {/* Create Payment Button */}
          <button
            type="submit"
            disabled={submitting}
            className="w-full bg-brand-600 hover:bg-brand-500 text-white font-bold py-4 rounded-xl text-base tracking-wide uppercase transition-all shadow-lg shadow-brand-600/30 flex items-center justify-center space-x-2 disabled:opacity-50"
          >
            {submitting ? (
              <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white" />
            ) : (
              <>
                <QrCode className="w-5 h-5" />
                <span>Create Payment</span>
              </>
            )}
          </button>
        </form>
      </div>
    </div>
  );
};
