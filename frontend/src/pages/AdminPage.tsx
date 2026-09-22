import React, { useState, useEffect } from 'react';
import { Shield, Activity, FileText, ArrowUpRight, AlertTriangle, Layers, CheckCircle2, Clock } from 'lucide-react';
import { paymentService } from '../services/paymentService';
import { StatusBadge } from '../components/StatusBadge';

export const AdminPage: React.FC = () => {
  const [metrics, setMetrics] = useState<Record<string, number>>({});
  const [activeTab, setActiveTab] = useState<'orders' | 'events' | 'audit'>('orders');
  const [orders, setOrders] = useState<any[]>([]);
  const [events, setEvents] = useState<any[]>([]);
  const [auditLogs, setAuditLogs] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchAdminData = async () => {
      try {
        const metricsRes = await paymentService.getAdminMetrics();
        setMetrics(metricsRes);

        const ordersRes = await paymentService.getAdminOrders(0, 10);
        setOrders(ordersRes.content);

        const eventsRes = await paymentService.getAdminEvents(0, 10);
        setEvents(eventsRes.content);

        const auditRes = await paymentService.getAdminAuditLogs(0, 10);
        setAuditLogs(auditRes.content);
      } catch (err) {
        console.error('Failed to load admin observability data:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchAdminData();
  }, []);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-8">
      {/* Header */}
      <div className="flex items-center space-x-3 pb-4 border-b border-slate-800">
        <div className="p-2.5 rounded-xl bg-amber-500/10 border border-amber-500/20 text-amber-400">
          <Shield className="w-6 h-6" />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-white tracking-tight">Admin Observability Panel</h1>
          <p className="text-slate-400 text-xs">
            Read-only system observability, webhook event auditing, and payment transaction logs.
          </p>
        </div>
      </div>

      {/* Metrics Row */}
      <div className="grid grid-cols-2 sm:grid-cols-5 gap-4">
        <div className="bg-slate-800/80 border border-slate-700/80 rounded-xl p-4 text-center">
          <span className="text-[10px] uppercase font-semibold text-slate-400 block mb-1">Total Orders</span>
          <span className="text-xl font-bold text-white">{metrics.totalOrders ?? 0}</span>
        </div>
        <div className="bg-slate-800/80 border border-slate-700/80 rounded-xl p-4 text-center">
          <span className="text-[10px] uppercase font-semibold text-blue-400 block mb-1">Pending</span>
          <span className="text-xl font-bold text-blue-400">{metrics.pendingOrders ?? 0}</span>
        </div>
        <div className="bg-slate-800/80 border border-slate-700/80 rounded-xl p-4 text-center">
          <span className="text-[10px] uppercase font-semibold text-amber-400 block mb-1">Partially Paid</span>
          <span className="text-xl font-bold text-amber-400">{metrics.partiallyPaidOrders ?? 0}</span>
        </div>
        <div className="bg-slate-800/80 border border-slate-700/80 rounded-xl p-4 text-center">
          <span className="text-[10px] uppercase font-semibold text-emerald-400 block mb-1">Completed</span>
          <span className="text-xl font-bold text-emerald-400">{metrics.completedOrders ?? 0}</span>
        </div>
        <div className="bg-slate-800/80 border border-slate-700/80 rounded-xl p-4 text-center col-span-2 sm:col-span-1">
          <span className="text-[10px] uppercase font-semibold text-rose-400 block mb-1">Failed</span>
          <span className="text-xl font-bold text-rose-400">{metrics.failedOrders ?? 0}</span>
        </div>
      </div>

      {/* Tabs */}
      <div className="bg-slate-800/80 border border-slate-700/80 rounded-2xl p-6 shadow-xl backdrop-blur-sm">
        <div className="flex items-center space-x-2 border-b border-slate-700 pb-4 mb-6">
          <button
            onClick={() => setActiveTab('orders')}
            className={`px-4 py-2 rounded-xl text-xs font-semibold transition-colors ${
              activeTab === 'orders'
                ? 'bg-brand-600 text-white shadow-sm'
                : 'bg-slate-900 text-slate-400 hover:text-white border border-slate-700'
            }`}
          >
            All Payment Orders
          </button>

          <button
            onClick={() => setActiveTab('events')}
            className={`px-4 py-2 rounded-xl text-xs font-semibold transition-colors ${
              activeTab === 'events'
                ? 'bg-brand-600 text-white shadow-sm'
                : 'bg-slate-900 text-slate-400 hover:text-white border border-slate-700'
            }`}
          >
            Inbound Webhook Events
          </button>

          <button
            onClick={() => setActiveTab('audit')}
            className={`px-4 py-2 rounded-xl text-xs font-semibold transition-colors ${
              activeTab === 'audit'
                ? 'bg-brand-600 text-white shadow-sm'
                : 'bg-slate-900 text-slate-400 hover:text-white border border-slate-700'
            }`}
          >
            Security Audit Trail
          </button>
        </div>

        {/* Tab 1: Orders */}
        {activeTab === 'orders' && (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="text-slate-400 uppercase tracking-wider border-b border-slate-700 pb-2">
                <tr>
                  <th className="py-2.5 px-3">Reference</th>
                  <th className="py-2.5 px-3">Recipient</th>
                  <th className="py-2.5 px-3">UPI ID</th>
                  <th className="py-2.5 px-3">Amount</th>
                  <th className="py-2.5 px-3">Paid</th>
                  <th className="py-2.5 px-3">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-700/50">
                {orders.map((o) => (
                  <tr key={o.id} className="hover:bg-slate-700/20">
                    <td className="py-3 px-3 font-mono text-slate-300">{o.orderReference}</td>
                    <td className="py-3 px-3 font-medium text-white">{o.recipientName}</td>
                    <td className="py-3 px-3 font-mono text-slate-400">{o.upiId}</td>
                    <td className="py-3 px-3 font-bold text-white">₹{o.totalAmount}</td>
                    <td className="py-3 px-3 text-emerald-400">₹{o.paidAmount}</td>
                    <td className="py-3 px-3">
                      <StatusBadge status={o.status} size="sm" />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Tab 2: Events */}
        {activeTab === 'events' && (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="text-slate-400 uppercase tracking-wider border-b border-slate-700 pb-2">
                <tr>
                  <th className="py-2.5 px-3">Event ID</th>
                  <th className="py-2.5 px-3">Type</th>
                  <th className="py-2.5 px-3">Provider</th>
                  <th className="py-2.5 px-3">Processed</th>
                  <th className="py-2.5 px-3">Timestamp</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-700/50">
                {events.map((e) => (
                  <tr key={e.id} className="hover:bg-slate-700/20">
                    <td className="py-3 px-3 font-mono text-slate-300">{e.eventId}</td>
                    <td className="py-3 px-3 font-semibold text-brand-300">{e.eventType}</td>
                    <td className="py-3 px-3 text-slate-400">{e.providerName}</td>
                    <td className="py-3 px-3">
                      <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                        e.processed ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30' : 'bg-amber-500/10 text-amber-400 border border-amber-500/30'
                      }`}>
                        {e.processed ? 'PROCESSED' : 'PENDING'}
                      </span>
                    </td>
                    <td className="py-3 px-3 text-slate-400">{new Date(e.createdAt).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Tab 3: Audit Logs */}
        {activeTab === 'audit' && (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="text-slate-400 uppercase tracking-wider border-b border-slate-700 pb-2">
                <tr>
                  <th className="py-2.5 px-3">Action</th>
                  <th className="py-2.5 px-3">Resource</th>
                  <th className="py-2.5 px-3">IP Address</th>
                  <th className="py-2.5 px-3">Details</th>
                  <th className="py-2.5 px-3">Timestamp</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-700/50">
                {auditLogs.map((a) => (
                  <tr key={a.id} className="hover:bg-slate-700/20">
                    <td className="py-3 px-3 font-semibold text-white">{a.action}</td>
                    <td className="py-3 px-3 text-slate-400">{a.resourceType}</td>
                    <td className="py-3 px-3 font-mono text-slate-400">{a.ipAddress}</td>
                    <td className="py-3 px-3 text-slate-300 truncate max-w-xs">{a.details}</td>
                    <td className="py-3 px-3 text-slate-400">{new Date(a.createdAt).toLocaleString()}</td>
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
