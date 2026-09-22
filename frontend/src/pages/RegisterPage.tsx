import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Mail, Lock, User as UserIcon, Building2, QrCode, ArrowRight, AlertCircle } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { Role } from '../types/auth';

export const RegisterPage: React.FC = () => {
  const { register } = useAuth();
  const navigate = useNavigate();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [role, setRole] = useState<Role>('MERCHANT');
  const [businessName, setBusinessName] = useState('');
  const [defaultUpiId, setDefaultUpiId] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      await register({
        email,
        password,
        fullName,
        role,
        businessName: role === 'MERCHANT' ? businessName : undefined,
        defaultUpiId: defaultUpiId || undefined,
      });
      navigate('/dashboard', { replace: true });
    } catch (err: any) {
      const msg = err.response?.data?.message || err.response?.data?.errors?.email || 'Registration failed. Please check your information.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-[80vh] flex items-center justify-center px-4 sm:px-6 lg:px-8 py-12">
      <div className="max-w-lg w-full bg-slate-800/90 border border-slate-700/80 rounded-3xl p-8 shadow-2xl backdrop-blur-xl">
        <div className="text-center mb-8">
          <div className="h-12 w-12 rounded-2xl bg-gradient-to-tr from-brand-600 to-indigo-400 flex items-center justify-center font-bold text-white text-xl mx-auto mb-4 shadow-md shadow-brand-500/20">
            SP
          </div>
          <h2 className="text-2xl font-bold text-white tracking-tight">Create SplitPay Account</h2>
          <p className="text-slate-400 text-sm mt-1">Start splitting UPI payment requests instantly</p>
        </div>

        {error && (
          <div className="mb-6 bg-rose-500/10 border border-rose-500/30 rounded-xl p-3.5 flex items-start space-x-2 text-rose-400 text-xs">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Account Role Selector */}
          <div className="grid grid-cols-2 gap-3 mb-2">
            <button
              type="button"
              onClick={() => setRole('MERCHANT')}
              className={`p-3 rounded-xl border text-xs font-semibold flex flex-col items-center justify-center transition-all ${
                role === 'MERCHANT'
                  ? 'bg-brand-600/20 border-brand-500 text-brand-300'
                  : 'bg-slate-900/60 border-slate-700 text-slate-400 hover:text-white'
              }`}
            >
              <Building2 className="w-4 h-4 mb-1" />
              <span>Merchant Account</span>
            </button>

            <button
              type="button"
              onClick={() => setRole('USER')}
              className={`p-3 rounded-xl border text-xs font-semibold flex flex-col items-center justify-center transition-all ${
                role === 'USER'
                  ? 'bg-brand-600/20 border-brand-500 text-brand-300'
                  : 'bg-slate-900/60 border-slate-700 text-slate-400 hover:text-white'
              }`}
            >
              <UserIcon className="w-4 h-4 mb-1" />
              <span>Personal Account</span>
            </button>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1" htmlFor="reg-name">
              Full Name
            </label>
            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-500">
                <UserIcon className="w-4 h-4" />
              </div>
              <input
                id="reg-name"
                type="text"
                required
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="Rajesh Sharma"
                className="w-full pl-10 pr-4 py-2 bg-slate-900/80 border border-slate-700 rounded-xl text-white text-sm focus:outline-none focus:border-brand-500"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1" htmlFor="reg-email">
              Email Address
            </label>
            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-500">
                <Mail className="w-4 h-4" />
              </div>
              <input
                id="reg-email"
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="merchant@splitpay.in"
                className="w-full pl-10 pr-4 py-2 bg-slate-900/80 border border-slate-700 rounded-xl text-white text-sm focus:outline-none focus:border-brand-500"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1" htmlFor="reg-password">
              Password (min 8 characters)
            </label>
            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-500">
                <Lock className="w-4 h-4" />
              </div>
              <input
                id="reg-password"
                type="password"
                required
                minLength={8}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="w-full pl-10 pr-4 py-2 bg-slate-900/80 border border-slate-700 rounded-xl text-white text-sm focus:outline-none focus:border-brand-500"
              />
            </div>
          </div>

          {role === 'MERCHANT' && (
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1" htmlFor="reg-business">
                Business / Store Name
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-500">
                  <Building2 className="w-4 h-4" />
                </div>
                <input
                  id="reg-business"
                  type="text"
                  value={businessName}
                  onChange={(e) => setBusinessName(e.target.value)}
                  placeholder="Sharma Electronics &amp; Appliances"
                  className="w-full pl-10 pr-4 py-2 bg-slate-900/80 border border-slate-700 rounded-xl text-white text-sm focus:outline-none focus:border-brand-500"
                />
              </div>
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1" htmlFor="reg-upi">
              Default Payee UPI ID (Optional)
            </label>
            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-500">
                <QrCode className="w-4 h-4" />
              </div>
              <input
                id="reg-upi"
                type="text"
                value={defaultUpiId}
                onChange={(e) => setDefaultUpiId(e.target.value)}
                placeholder="sharma@upi"
                className="w-full pl-10 pr-4 py-2 bg-slate-900/80 border border-slate-700 rounded-xl text-white text-sm focus:outline-none focus:border-brand-500 font-mono"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-brand-600 hover:bg-brand-500 text-white font-semibold py-3 rounded-xl text-sm transition-all shadow-md shadow-brand-600/30 flex items-center justify-center space-x-2 mt-6 disabled:opacity-50"
          >
            {loading ? (
              <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white" />
            ) : (
              <>
                <span>Complete Registration</span>
                <ArrowRight className="w-4 h-4" />
              </>
            )}
          </button>
        </form>

        <div className="mt-8 pt-6 border-t border-slate-700/60 text-center text-xs text-slate-400">
          Already have an account?{' '}
          <Link to="/login" className="text-brand-400 hover:text-brand-300 font-semibold">
            Sign In
          </Link>
        </div>
      </div>
    </div>
  );
};
