import React from 'react';
import { ShieldCheck, Info } from 'lucide-react';

export const Footer: React.FC = () => {
  return (
    <footer className="bg-slate-950 border-t border-slate-800 py-10 mt-auto no-print">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-4 mb-8 flex items-start space-x-3 text-xs text-slate-400">
          <Info className="w-5 h-5 text-brand-400 shrink-0 mt-0.5" />
          <div>
            <p className="font-semibold text-slate-200 mb-1">
              Important Regulatory & Payment Architecture Disclosure
            </p>
            <p className="leading-relaxed">
              SPLITPAY formats standards-compliant NPCI UPI payment requests (<code className="text-brand-300">upi://pay</code>).
              Generation and display of a QR code or payment payload constitutes the creation of a payment request only.
              It does NOT imply authoritative settlement or money receipt. Settlement requires verified confirmation through an
              authorized payment aggregator/banking partner via authoritative webhook or reconciliation feeds.
              SPLITPAY never stores banking passwords, UPI PINs, or OTPs.
            </p>
          </div>
        </div>

        <div className="flex flex-col sm:flex-row items-center justify-between text-xs text-slate-500">
          <div className="flex items-center space-x-2 mb-4 sm:mb-0">
            <ShieldCheck className="w-4 h-4 text-emerald-500" />
            <span>Production-Oriented UPI Splitting Platform — India</span>
          </div>
          <div>
            &copy; {new Date().getFullYear()} SplitPay Inc. Built with Spring Boot 3 &amp; React.
          </div>
        </div>
      </div>
    </footer>
  );
};
