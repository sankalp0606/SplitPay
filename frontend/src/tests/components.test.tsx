import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { StatusBadge } from '../components/StatusBadge';
import { ReconciliationBanner } from '../components/ReconciliationBanner';
import { QrCard } from '../components/QrCard';
import { PaymentPart } from '../types/payment';

describe('StatusBadge Component', () => {
  it('renders PENDING status badge correctly', () => {
    render(<StatusBadge status="PENDING" />);
    expect(screen.getByText('PENDING')).toBeInTheDocument();
  });

  it('renders SUCCESS status badge correctly', () => {
    render(<StatusBadge status="SUCCESS" />);
    expect(screen.getByText('SUCCESS')).toBeInTheDocument();
  });

  it('renders PARTIALLY_PAID status badge correctly', () => {
    render(<StatusBadge status="PARTIALLY_PAID" />);
    expect(screen.getByText('PARTIALLY PAID')).toBeInTheDocument();
  });

  it('renders COMPLETED status badge correctly', () => {
    render(<StatusBadge status="COMPLETED" />);
    expect(screen.getByText('COMPLETED')).toBeInTheDocument();
  });
});

describe('ReconciliationBanner Component', () => {
  it('accurately renders expected, paid, and remaining balances for ₹5,000 partial payment', () => {
    render(
      <ReconciliationBanner
        totalAmount={5000}
        paidAmount={1990}
        remainingAmount={3010}
        status="PARTIALLY_PAID"
      />
    );

    // Total Expected
    expect(screen.getAllByText(/5,000.00/).length).toBeGreaterThan(0);
    // Total Paid
    expect(screen.getByText('₹1,990.00')).toBeInTheDocument();
    // Remaining Balance
    expect(screen.getByText('₹3,010.00')).toBeInTheDocument();
    // Status Badge
    expect(screen.getByText('PARTIALLY PAID')).toBeInTheDocument();
  });

  it('accurately displays 100% completion when fully settled', () => {
    render(
      <ReconciliationBanner
        totalAmount={5000}
        paidAmount={5000}
        remainingAmount={0}
        status="COMPLETED"
      />
    );

    expect(screen.getByText('COMPLETED')).toBeInTheDocument();
    expect(screen.getByText('₹0.00')).toBeInTheDocument();
  });
});

describe('QrCard Component', () => {
  const mockPart: PaymentPart = {
    id: 'part-123',
    paymentOrderId: 'order-123',
    partNumber: 1,
    paymentReference: 'SP-17900-P1-492',
    amount: 1990,
    currency: 'INR',
    status: 'PENDING',
    upiUri: 'upi://pay?pa=abcelectronics@upi&pn=ABC%20Electronics&am=1990.00&cu=INR&tr=SP-17900-P1-492',
    qrDataUri: 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  };

  it('renders part number, amount, reference and UPI deep link', () => {
    render(
      <QrCard
        part={mockPart}
        upiId="abcelectronics@upi"
        recipientName="ABC Electronics"
      />
    );

    expect(screen.getByText('Payment Part 1')).toBeInTheDocument();
    expect(screen.getByText('₹1,990.00')).toBeInTheDocument();
    expect(screen.getByText(/Ref: SP-17900-P1-492/)).toBeInTheDocument();
    expect(screen.getByText('PENDING')).toBeInTheDocument();
    expect(screen.getByText('Pay via UPI App')).toBeInTheDocument();
    expect(screen.getByText('Copy UPI')).toBeInTheDocument();
    expect(screen.getByText('Download QR')).toBeInTheDocument();
  });
});
