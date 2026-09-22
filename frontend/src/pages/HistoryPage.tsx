import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Search, Filter, ArrowUpRight, ChevronLeft, ChevronRight, History } from 'lucide-react';
import { paymentService } from '../services/paymentService';
import { PaymentOrder, PaymentOrderStatus } from '../types/payment';
import { StatusBadge } from '../components/StatusBadge';

export const HistoryPage: React.FC = () => {
  const [orders, setOrders] = useState<PaymentOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [searchTerm, setSearchTerm] = useState('');

  const fetchOrders = async (targetPage = 0) => {
    setLoading(true);
    try {
      const res = await paymentService.getOrders(targetPage, 10);
      setOrders(res.content);
      setTotalPages(res.totalPages || 1);
      setPage(res.number || 0);
    } catch (err) {
      console.error('Failed to load order history:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrders(page);
  }, [page]);

  const filteredOrders = orders.filter((order) => {
    const matchesStatus = statusFilter === 'ALL' || order.status === statusFilter;
    const matchesSearch =
      order.orderReference.toLowerCase().includes(searchTerm.toLowerCase()) ||
      order.recipientName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      order.upiId.toLowerCase().includes(searchTerm.toLowerCase());
    return matchesStatus && matchesSearch;
  });

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">Payment History</h1>
          <p className="text-slate-400 text-sm mt-1">Review all your payment orders and settlement statuses.</p>
        </div>
      </div>

      {/* Filter & Search Bar */}
      <div className="bg-slate-800/80 border border-slate-700/80 rounded-2xl p-4 shadow-lg flex flex-col sm:flex-row gap-3 justify-between items-center">
        <div className="relative w-full sm:w-80">
          <Search className="w-4 h-4 absolute left-3.5 top-3 text-slate-500" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search reference, recipient, UPI..."
            className="w-full pl-10 pr-4 py-2 bg-slate-900 border border-slate-700 rounded-xl text-white text-xs focus:outline-none focus:border-brand-500"
          />
        </div>

        <div className="flex items-center space-x-2 w-full sm:w-auto overflow-x-auto pb-1 sm:pb-0">
          {['ALL', 'PENDING', 'PARTIALLY_PAID', 'COMPLETED', 'FAILED'].map((st) => (
            <button
              key={st}
              onClick={() => setStatusFilter(st)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-colors ${
                statusFilter === st
                  ? 'bg-brand-600 text-white shadow-sm shadow-brand-600/30'
                  : 'bg-slate-900 text-slate-400 border border-slate-700 hover:text-white'
              }`}
            >
              {st === 'ALL' ? 'All Orders' : st.replace('_', ' ')}
            </button>
          ))}
        </div>
      </div>

      {/* Table */}
      <div className="bg-slate-800/80 border border-slate-700/80 rounded-2xl p-6 shadow-xl backdrop-blur-sm">
        {loading ? (
          <div className="py-16 flex justify-center">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-brand-500" />
          </div>
        ) : filteredOrders.length === 0 ? (
          <div className="text-center py-16 text-slate-400 text-sm">
            No matching payment orders found.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="text-slate-400 uppercase tracking-wider border-b border-slate-700/60 pb-3">
                <tr>
                  <th className="py-3 px-3 font-semibold">Reference</th>
                  <th className="py-3 px-3 font-semibold">Created Date</th>
                  <th className="py-3 px-3 font-semibold">Recipient</th>
                  <th className="py-3 px-3 font-semibold">Payee UPI</th>
                  <th className="py-3 px-3 font-semibold">Total Amount</th>
                  <th className="py-3 px-3 font-semibold">Parts</th>
                  <th className="py-3 px-3 font-semibold">Status</th>
                  <th className="py-3 px-3 font-semibold text-right">Details</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-700/40">
                {filteredOrders.map((order) => (
                  <tr key={order.id} className="hover:bg-slate-700/20 transition-colors">
                    <td className="py-3.5 px-3 font-mono text-slate-300">{order.orderReference}</td>
                    <td className="py-3.5 px-3 text-slate-400">{new Date(order.createdAt).toLocaleDateString()}</td>
                    <td className="py-3.5 px-3 font-medium text-white">{order.recipientName}</td>
                    <td className="py-3.5 px-3 font-mono text-slate-400">{order.upiId}</td>
                    <td className="py-3.5 px-3 font-bold text-white">
                      ₹{order.totalAmount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                    </td>
                    <td className="py-3.5 px-3 text-slate-300">{order.partCount} parts</td>
                    <td className="py-3.5 px-3">
                      <StatusBadge status={order.status} size="sm" />
                    </td>
                    <td className="py-3.5 px-3 text-right">
                      <Link
                        to={`/payment/${order.id}`}
                        className="text-brand-400 hover:text-brand-300 font-semibold inline-flex items-center space-x-1"
                      >
                        <span>View</span>
                        <ArrowUpRight className="w-3 h-3" />
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        <div className="flex items-center justify-between pt-6 border-t border-slate-700/60 text-xs text-slate-400">
          <span>Page {page + 1} of {totalPages}</span>
          <div className="flex items-center space-x-2">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="p-1.5 rounded-lg bg-slate-900 border border-slate-700 hover:text-white disabled:opacity-40 transition-colors"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <button
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1}
              className="p-1.5 rounded-lg bg-slate-900 border border-slate-700 hover:text-white disabled:opacity-40 transition-colors"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
