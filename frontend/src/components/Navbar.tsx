import React from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { Layers, PlusCircle, History, Shield, LogOut, User as UserIcon } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export const Navbar: React.FC = () => {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const isActive = (path: string) => location.pathname === path;

  return (
    <header className="sticky top-0 z-40 bg-slate-900/90 backdrop-blur-md border-b border-slate-800">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        {/* Brand */}
        <Link to="/" className="flex items-center space-x-3 group">
          <div className="h-9 w-9 rounded-xl bg-gradient-to-tr from-brand-600 to-indigo-400 flex items-center justify-center font-bold text-white shadow-md shadow-brand-500/20 group-hover:scale-105 transition-transform">
            SP
          </div>
          <div className="flex flex-col">
            <span className="font-bold text-lg text-white tracking-tight leading-none">SPLITPAY</span>
            <span className="text-[10px] text-slate-400 tracking-wider uppercase font-medium">UPI Split Engine</span>
          </div>
        </Link>

        {/* Navigation links */}
        {isAuthenticated ? (
          <nav className="flex items-center space-x-1 sm:space-x-3">
            <Link
              to="/dashboard"
              className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors flex items-center space-x-1.5 ${
                isActive('/dashboard') ? 'bg-brand-600/20 text-brand-300 border border-brand-500/30' : 'text-slate-300 hover:text-white hover:bg-slate-800/60'
              }`}
            >
              <Layers className="w-4 h-4" />
              <span className="hidden sm:inline">Dashboard</span>
            </Link>

            <Link
              to="/create-payment"
              className={`px-3.5 py-1.5 rounded-lg text-sm font-medium transition-colors flex items-center space-x-1.5 ${
                isActive('/create-payment') ? 'bg-brand-600 text-white shadow-md shadow-brand-600/30' : 'bg-brand-600/90 hover:bg-brand-500 text-white'
              }`}
            >
              <PlusCircle className="w-4 h-4" />
              <span>Create Payment</span>
            </Link>

            <Link
              to="/history"
              className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors flex items-center space-x-1.5 ${
                isActive('/history') ? 'bg-brand-600/20 text-brand-300 border border-brand-500/30' : 'text-slate-300 hover:text-white hover:bg-slate-800/60'
              }`}
            >
              <History className="w-4 h-4" />
              <span className="hidden sm:inline">History</span>
            </Link>

            {user?.role === 'ADMIN' && (
              <Link
                to="/admin"
                className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors flex items-center space-x-1.5 ${
                  isActive('/admin') ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30' : 'text-amber-400/90 hover:text-amber-300 hover:bg-slate-800/60'
                }`}
              >
                <Shield className="w-4 h-4" />
                <span className="hidden sm:inline">Admin</span>
              </Link>
            )}

            {/* Profile Dropdown / Actions */}
            <div className="flex items-center pl-2 border-l border-slate-800 space-x-2">
              <Link
                to="/profile"
                className="p-2 text-slate-400 hover:text-white hover:bg-slate-800 rounded-lg transition-colors"
                title="Profile settings"
              >
                <UserIcon className="w-4 h-4" />
              </Link>

              <button
                onClick={handleLogout}
                className="p-2 text-slate-400 hover:text-rose-400 hover:bg-slate-800 rounded-lg transition-colors"
                title="Sign out"
              >
                <LogOut className="w-4 h-4" />
              </button>
            </div>
          </nav>
        ) : (
          <div className="flex items-center space-x-3">
            <Link
              to="/login"
              className="text-sm font-medium text-slate-300 hover:text-white px-3 py-1.5 rounded-lg hover:bg-slate-800/60 transition-colors"
            >
              Sign In
            </Link>
            <Link
              to="/register"
              className="text-sm font-medium bg-brand-600 hover:bg-brand-500 text-white px-4 py-1.5 rounded-lg transition-colors shadow-sm shadow-brand-500/20"
            >
              Get Started
            </Link>
          </div>
        )}
      </div>
    </header>
  );
};
