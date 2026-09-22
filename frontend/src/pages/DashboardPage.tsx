import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { PlusCircle, ArrowUpRight, Clock, CheckCircle2, AlertCircle, Layers } from 'lucide-react';
import { paymentService } from '../services/paymentService';
import { PaymentOrder } from '../types/payment';
import { StatusBadge } from '../components/StatusBadge';
import { useAuth } from '../context/AuthContext';

export const DashboardPage: React.FC = () => {
  const { user } = useAuth();
  const [orders, setOrders] = useState<PaymentOrder[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchDashboardData = async () => {
      try {
        const res = await paymentService.getOrders(0, 5);
        setOrders(res.content);
      } catch (err) {
        console.error('Failed to load dashboard orders:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchDashboardData();
  }, []);

  const totalVolume = orders.reduce((sum, o) => sum + o.totalAmount, 0);
  const totalSettled = orders.reduce((sum, o) => sum + o.paidAmount, 0);
  const pendingOrdersCount = orders.filter((o) => o.status === 'PENDING' || o.status === 'PARTIALLY_PAID').length;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-8">
      {/* Welcome Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
            Dashboard
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Welcome back, {user?.fullName}. Here is your UPI payment split activity overview.
          </p>
        </div>

        <Link
          to="/create-payment"
          className="inline-flex items-center space-x-2 px-5 py-2.5 bg-brand-600 hover:bg-brand-500 text-white rounded-xl text-sm font-semibold transition-all shadow-md shadow-brand-600/30 self-start sm:self-auto"
        >
          <PlusCircle className="w-4 h-4" />
          <span>New Payment</span>
        </Link>
      </div>

      {/* Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-5">
        <div className="bg-slate-800/80 border border-slate-700/80 rounded-2xl p-6 shadow-lg">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">Total Volume Split</span>
            <Layers className="w-4 h-4 text-brand-400" />
          </div>
          <div className="text-2xl sm:text-3xl font-bold text-white">
            ₹{totalVolume.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
          </div>
          <span className="text-[11px] text-slate-500 mt-1 block">Across recent payment orders</span>
        </div>

        <div className="bg-slate-800/80 border border-slate-700/80 rounded-2xl p-6 shadow-lg">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">Settled &amp; Reconciled</span>
            <CheckCircle2 className="w-4 h-4 text-emerald-400" />
          </div>
          <div className="text-2xl sm:text-3xl font-bold text-emerald-400">
            ₹{totalSettled.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
          </div>
          <span className="text-[11px] text-slate-500 mt-1 block">Authoritatively verified</span>
        </div>

        <div className="bg-slate-800/80 border border-slate-700/80 rounded-2xl p-6 shadow-lg">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">Active / Pending</span>
            <Clock className="w-4 h-4 text-amber-400" />
          </div>
          <div className="text-2xl sm:text-3xl font-bold text-amber-400">
            {pendingOrdersCount} Orders
          </div>
          <span className="text-[11px] text-slate-500 mt-1 block">Awaiting customer payment</span>
        </div>
      </div>

      {/* Recent Orders Section */}
      <div className="bg-slate-800/80 border border-slate-700/80 rounded-2xl p-6 shadow-xl backdrop-blur-sm">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-lg font-bold text-white">Recent Payment Orders</h2>
          <Link to="/history" className="text-xs font-semibold text-brand-400 hover:text-brand-300 flex items-center space-x-1">
            <span>View All</span>
            <ArrowUpRight className="w-3.5 h-3.5" />
          </Link>
        </div>

        {loading ? (
          <div className="py-12 flex justify-center">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-brand-500" />
          </div>
        ) : orders.length === 0 ? (
          <div className="text-center py-12">
            <p className="text-slate-400 text-sm mb-4">No payment orders created yet.</p>
            <Link
              to="/create-payment"
              className="inline-flex items-center space-x-2 px-4 py-2 bg-brand-600 hover:bg-brand-500 text-white rounded-xl text-xs font-semibold"
            >
              <PlusCircle className="w-3.5 h-3.5" />
              <span>Create Your First Payment</span>
            </Link>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="text-slate-400 uppercase tracking-wider border-b border-slate-700/60 pb-3">
                <tr>
                  <th className="py-3 px-3 font-semibold">Reference</th>
                  <th className="py-3 px-3 font-semibold">Recipient</th>
                  <th className="py-3 px-3 font-semibold">Payee UPI</th>
                  <th className="py-3 px-3 font-semibold">Total Amount</th>
                  <th className="py-3 px-3 font-semibold">Parts</th>
                  <th className="py-3 px-3 font-semibold">Status</th>
                  <th className="py-3 px-3 font-semibold text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-700/40">
                {orders.map((order) => (
                  <tr key={order.id} className="hover:bg-slate-700/20 transition-colors">
                    <td className="py-3.5 px-3 font-mono text-slate-300">{order.orderReference}</td>
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
      </div>
    </div>
  );
};
