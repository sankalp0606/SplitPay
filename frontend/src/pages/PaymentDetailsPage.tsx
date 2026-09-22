import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ArrowLeft, Printer, Share2, RefreshCw, Check, AlertCircle, QrCode } from 'lucide-react';
import { paymentService } from '../services/paymentService';
import { PaymentOrder } from '../types/payment';
import { ReconciliationBanner } from '../components/ReconciliationBanner';
import { QrCard } from '../components/QrCard';

export const PaymentDetailsPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();

  const [order, setOrder] = useState<PaymentOrder | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [copiedLink, setCopiedLink] = useState(false);

  const fetchOrder = async (isManualRefresh = false) => {
    if (!id) return;
    if (isManualRefresh) setRefreshing(true);
    try {
      const data = await paymentService.getOrderById(id);
      setOrder(data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load payment order.');
    } finally {
      setLoading(false);
      if (isManualRefresh) setRefreshing(false);
    }
  };

  useEffect(() => {
    fetchOrder();

    // Polling interval to query authoritative status changes from backend
    const interval = setInterval(() => {
      if (order && order.status !== 'COMPLETED' && order.status !== 'FAILED') {
        fetchOrder();
      }
    }, 5000);

    return () => clearInterval(interval);
  }, [id, order?.status]);

  const handleCopyShareLink = () => {
    navigator.clipboard.writeText(window.location.href);
    setCopiedLink(true);
    setTimeout(() => setCopiedLink(false), 2000);
  };

  const handlePrint = () => {
    window.print();
  };

  if (loading) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-brand-500" />
      </div>
    );
  }

  if (error || !order) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-16 text-center">
        <div className="bg-rose-500/10 border border-rose-500/30 rounded-2xl p-8 mb-6">
          <AlertCircle className="w-10 h-10 text-rose-400 mx-auto mb-3" />
          <h2 className="text-xl font-bold text-white mb-2">Order Not Found</h2>
          <p className="text-sm text-slate-400 mb-6">{error || 'The requested payment order does not exist.'}</p>
          <Link
            to="/create-payment"
            className="inline-flex items-center space-x-2 px-5 py-2.5 bg-brand-600 hover:bg-brand-500 text-white rounded-xl text-sm font-semibold transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Create New Payment</span>
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-8">
      {/* Top Bar Navigation & Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-800 no-print">
        <Link
          to="/dashboard"
          className="inline-flex items-center space-x-2 text-sm text-slate-400 hover:text-white transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Back to Dashboard</span>
        </Link>

        <div className="flex items-center space-x-2">
          <button
            onClick={() => fetchOrder(true)}
            disabled={refreshing}
            className="px-3.5 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold border border-slate-700 flex items-center space-x-1.5 transition-colors disabled:opacity-50"
            title="Refresh payment status from backend"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${refreshing ? 'animate-spin' : ''}`} />
            <span>Refresh Status</span>
          </button>

          <button
            onClick={handleCopyShareLink}
            className="px-3.5 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold border border-slate-700 flex items-center space-x-1.5 transition-colors"
          >
            {copiedLink ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Share2 className="w-3.5 h-3.5" />}
            <span>{copiedLink ? 'Link Copied' : 'Share Request'}</span>
          </button>

          <button
            onClick={handlePrint}
            className="px-3.5 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold border border-slate-700 flex items-center space-x-1.5 transition-colors"
          >
            <Printer className="w-3.5 h-3.5" />
            <span>Print Plan</span>
          </button>
        </div>
      </div>

      {/* Recipient & Payment Summary Card */}
      <div className="bg-slate-800/80 border border-slate-700/80 rounded-2xl p-6 shadow-xl backdrop-blur-sm">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div>
            <span className="text-xs text-slate-400 uppercase tracking-wider block mb-1">Recipient Merchant</span>
            <div className="text-lg font-bold text-white">{order.recipientName}</div>
          </div>
          <div>
            <span className="text-xs text-slate-400 uppercase tracking-wider block mb-1">Payee UPI ID</span>
            <div className="text-lg font-bold text-brand-300 font-mono">{order.upiId}</div>
          </div>
          <div>
            <span className="text-xs text-slate-400 uppercase tracking-wider block mb-1">Order Reference</span>
            <div className="text-sm font-mono text-slate-300">{order.orderReference}</div>
          </div>
        </div>
      </div>

      {/* Authoritative Reconciliation Status Banner */}
      <ReconciliationBanner
        totalAmount={order.totalAmount}
        paidAmount={order.paidAmount}
        remainingAmount={order.remainingAmount}
        status={order.status}
      />

      {/* Payment Parts Section */}
      <div>
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center space-x-2">
            <QrCode className="w-5 h-5 text-brand-400" />
            <h3 className="text-xl font-bold text-white">Payment Parts ({order.parts.length})</h3>
          </div>
          <span className="text-xs text-slate-400">Scan each QR code sequentially using your UPI app</span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {order.parts.map((part) => (
            <QrCard
              key={part.id}
              part={part}
              upiId={order.upiId}
              recipientName={order.recipientName}
            />
          ))}
        </div>
      </div>
    </div>
  );
};
