import React from 'react';
import { User, Building2, Mail, QrCode, Shield, CheckCircle } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export const ProfilePage: React.FC = () => {
  const { user } = useAuth();

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-8">
      <div>
        <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">Account Profile</h1>
        <p className="text-slate-400 text-sm mt-1">Manage your merchant details and UPI configuration.</p>
      </div>

      <div className="bg-slate-800/80 border border-slate-700/80 rounded-2xl p-6 sm:p-8 shadow-xl backdrop-blur-sm space-y-6">
        <div className="flex items-center space-x-4 pb-6 border-b border-slate-700/60">
          <div className="h-16 w-16 rounded-2xl bg-gradient-to-tr from-brand-600 to-indigo-400 flex items-center justify-center font-bold text-white text-2xl shadow-lg shadow-brand-500/20">
            {user?.fullName?.charAt(0) || 'U'}
          </div>
          <div>
            <h2 className="text-xl font-bold text-white">{user?.fullName}</h2>
            <div className="inline-flex items-center space-x-1.5 text-xs text-brand-300 bg-brand-500/10 px-2.5 py-0.5 rounded-full border border-brand-500/20 mt-1">
              <Shield className="w-3 h-3" />
              <span>Role: {user?.role}</span>
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
          <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-4">
            <div className="flex items-center space-x-2 text-slate-400 text-xs mb-1">
              <Mail className="w-4 h-4 text-brand-400" />
              <span>Registered Email</span>
            </div>
            <div className="text-sm font-semibold text-white">{user?.email}</div>
          </div>

          <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-4">
            <div className="flex items-center space-x-2 text-slate-400 text-xs mb-1">
              <Building2 className="w-4 h-4 text-brand-400" />
              <span>Business / Store Name</span>
            </div>
            <div className="text-sm font-semibold text-white">{user?.businessName || 'N/A (Personal Account)'}</div>
          </div>

          <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-4 sm:col-span-2">
            <div className="flex items-center space-x-2 text-slate-400 text-xs mb-1">
              <QrCode className="w-4 h-4 text-brand-400" />
              <span>Default Payee UPI ID</span>
            </div>
            <div className="text-sm font-mono font-semibold text-brand-300">
              {user?.defaultUpiId || 'None configured (Set upon payment creation)'}
            </div>
          </div>
        </div>

        <div className="p-4 bg-slate-900/40 border border-slate-800 rounded-xl flex items-start space-x-3 text-xs text-slate-400">
          <CheckCircle className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
          <p>
            Security Notice: SplitPay uses stateless cryptographically signed JWT tokens and does not hold any bank credentials, UPI PINs, or secret keys.
          </p>
        </div>
      </div>
    </div>
  );
};
