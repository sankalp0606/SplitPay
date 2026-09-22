import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, QrCode, ShieldCheck, Split, Zap, Smartphone, CheckCircle } from 'lucide-react';
import { paymentService } from '../services/paymentService';

export const LandingPage: React.FC = () => {
  const [testAmount, setTestAmount] = useState<number>(5000);
  const [previewParts, setPreviewParts] = useState<number[]>([1990, 1990, 1020]);

  const handleAmountChange = async (amt: number) => {
    setTestAmount(amt);
    if (amt > 0) {
      try {
        const res = await paymentService.previewPlan({ totalAmount: amt, maxPartAmount: 1990 });
        setPreviewParts(res.parts);
      } catch {
        // Fallback calculation for demonstration
        const parts: number[] = [];
        let rem = amt;
        while (rem > 1990) {
          parts.push(1990);
          rem -= 1990;
        }
        if (rem > 0) parts.push(Number(rem.toFixed(2)));
        setPreviewParts(parts);
      }
    }
  };

  return (
    <div className="space-y-20 pb-16">
      {/* Hero Section */}
      <section className="relative overflow-hidden pt-12 lg:pt-20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center relative z-10">
          <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-brand-500/10 border border-brand-500/20 text-brand-300 text-xs font-semibold mb-8">
            <Zap className="w-3.5 h-3.5 text-brand-400" />
            <span>Next-Gen UPI Payment Request Splitting</span>
          </div>

          <h1 className="text-4xl sm:text-6xl font-extrabold text-white tracking-tight leading-tight max-w-4xl mx-auto mb-6">
            Split High-Value UPI Payments into <span className="bg-gradient-to-r from-brand-400 to-indigo-300 bg-clip-text text-transparent">Frictionless Parts</span>
          </h1>

          <p className="text-lg sm:text-xl text-slate-300 max-w-2xl mx-auto mb-10 leading-relaxed">
            Eliminate bank-imposed single-transaction limits and payment frictions.
            Generate compliant UPI payment parts and QR codes in 3 seconds.
          </p>

          <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
            <Link
              to="/create-payment"
              className="w-full sm:w-auto px-8 py-3.5 rounded-xl bg-brand-600 hover:bg-brand-500 text-white font-semibold text-base shadow-lg shadow-brand-600/30 flex items-center justify-center space-x-2 transition-all"
            >
              <span>Create Payment Now</span>
              <ArrowRight className="w-4 h-4" />
            </Link>

            <Link
              to="/register"
              className="w-full sm:w-auto px-8 py-3.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 font-semibold text-base border border-slate-700 transition-all"
            >
              Create Free Merchant Account
            </Link>
          </div>
        </div>
      </section>

      {/* Interactive Splitting Demo */}
      <section className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="bg-slate-800/80 border border-slate-700 rounded-3xl p-6 sm:p-10 shadow-2xl backdrop-blur-xl">
          <div className="text-center max-w-xl mx-auto mb-8">
            <h2 className="text-2xl font-bold text-white mb-2">Experience the Splitting Engine</h2>
            <p className="text-slate-400 text-sm">
              See how SplitPay automatically calculates optimum parts below the configured threshold (e.g. ₹1,990).
            </p>
          </div>

          <div className="flex flex-wrap justify-center gap-2 mb-8">
            {[2000, 5000, 7500, 10000, 15000].map((amt) => (
              <button
                key={amt}
                onClick={() => handleAmountChange(amt)}
                className={`px-4 py-2 rounded-xl text-xs font-semibold transition-all ${
                  testAmount === amt
                    ? 'bg-brand-600 text-white shadow-md shadow-brand-600/20 border border-brand-500'
                    : 'bg-slate-900/60 text-slate-400 border border-slate-700 hover:text-white'
                }`}
              >
                ₹{amt.toLocaleString('en-IN')}
              </button>
            ))}
          </div>

          <div className="bg-slate-950/60 border border-slate-800 rounded-2xl p-6">
            <div className="flex items-center justify-between mb-4 pb-3 border-b border-slate-800">
              <span className="text-xs font-medium text-slate-400">Calculated Payment Parts</span>
              <span className="text-xs font-mono text-emerald-400">Total: ₹{testAmount.toLocaleString('en-IN')}</span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
              {previewParts.map((part, index) => (
                <div key={index} className="bg-slate-900 border border-slate-800 rounded-xl p-4 text-center">
                  <span className="text-[10px] font-semibold text-slate-400 uppercase block mb-1">Part {index + 1}</span>
                  <div className="text-xl font-bold text-white mb-2">₹{part.toLocaleString('en-IN')}</div>
                  <div className="inline-flex items-center space-x-1 text-[11px] text-brand-400 bg-brand-500/10 px-2 py-0.5 rounded-full border border-brand-500/20">
                    <QrCode className="w-3 h-3" />
                    <span>UPI QR Ready</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* Core Workflow Steps */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center max-w-2xl mx-auto mb-16">
          <h2 className="text-3xl font-bold text-white mb-3">Ultra-Simple User Flow</h2>
          <p className="text-slate-400 text-base">Designed for instant customer checkout with standard UPI apps.</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          <div className="bg-slate-800/60 border border-slate-700/60 rounded-2xl p-6 relative">
            <div className="h-12 w-12 rounded-xl bg-brand-600/20 border border-brand-500/30 text-brand-400 font-bold flex items-center justify-center text-lg mb-4">
              1
            </div>
            <h3 className="text-lg font-semibold text-white mb-2">Enter Essential Fields</h3>
            <p className="text-sm text-slate-400 leading-relaxed">
              Input recipient name, merchant UPI ID (e.g. <code className="text-slate-300">abcelectronics@upi</code>), and the total amount.
            </p>
          </div>

          <div className="bg-slate-800/60 border border-slate-700/60 rounded-2xl p-6 relative">
            <div className="h-12 w-12 rounded-xl bg-brand-600/20 border border-brand-500/30 text-brand-400 font-bold flex items-center justify-center text-lg mb-4">
              2
            </div>
            <h3 className="text-lg font-semibold text-white mb-2">Instant Smart Split</h3>
            <p className="text-sm text-slate-400 leading-relaxed">
              The engine divides ₹5,000 into compliant parts (₹1,990 + ₹1,990 + ₹1,020) and generates unique QR codes.
            </p>
          </div>

          <div className="bg-slate-800/60 border border-slate-700/60 rounded-2xl p-6 relative">
            <div className="h-12 w-12 rounded-xl bg-brand-600/20 border border-brand-500/30 text-brand-400 font-bold flex items-center justify-center text-lg mb-4">
              3
            </div>
            <h3 className="text-lg font-semibold text-white mb-2">Scan &amp; Reconcile</h3>
            <p className="text-sm text-slate-400 leading-relaxed">
              Customers scan each part using GPay, PhonePe, Paytm, or BHIM. Live status tracks settlement authoritatively.
            </p>
          </div>
        </div>
      </section>
    </div>
  );
};
