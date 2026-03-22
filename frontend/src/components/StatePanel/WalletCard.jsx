import { useState, useEffect, useRef } from 'react';
import { createStripeCheckout } from '../../utils/api';

export default function WalletCard({ wallet, userId }) {
    const [showHistory, setShowHistory] = useState(false);
    const [displayBalance, setDisplayBalance] = useState(0);
    const [loadingStripe, setLoadingStripe] = useState(false);
    const prevBalanceRef = useRef(0);

    const balance = wallet?.balance ?? 0;
    // Use the explicitly passed userId — don't rely on wallet.userId
    const effectiveUserId = userId || wallet?.userId;
    const transactions = wallet?.transactionHistory ?? [];

    // Animated balance counter
    useEffect(() => {
        const prev = prevBalanceRef.current;
        const diff = balance - prev;
        if (diff === 0 && displayBalance === balance) return;

        const steps = 30;
        const stepValue = diff / steps;
        let current = prev;
        let frame = 0;

        const interval = setInterval(() => {
            frame++;
            current += stepValue;
            if (frame >= steps) {
                current = balance;
                clearInterval(interval);
            }
            setDisplayBalance(current);
        }, 16);

        prevBalanceRef.current = balance;
        return () => clearInterval(interval);
    }, [balance]);

    function parseTx(tx) {
        if (!tx || typeof tx !== 'string') return { type: 'unknown', label: '—', amount: null };
        if (tx.startsWith('DEBIT')) return { type: 'debit', label: tx.replace(/^DEBIT[:\s]*/, ''), amount: null };
        if (tx.startsWith('REFUND')) return { type: 'refund', label: tx.replace(/^REFUND[:\s]*/, ''), amount: null };
        if (tx.startsWith('CREDIT')) return { type: 'credit', label: tx.replace(/^CREDIT[:\s]*/, ''), amount: null };
        return { type: 'unknown', label: tx, amount: null };
    }

    const txIconMap = { debit: '↑', credit: '↓', refund: '↩', unknown: '•' };

    const handleTopUp = async () => {
        if (!effectiveUserId) {
            alert('Please log in again to add money.');
            return;
        }
        setLoadingStripe(true);
        try {
            const data = await createStripeCheckout(effectiveUserId, 500);
            if (data.url) {
                window.location.href = data.url;
            } else {
                alert('Stripe error: ' + (data.error || 'Unknown error'));
            }
        } catch (e) {
            alert('Payment setup failed: ' + e.message);
        } finally {
            setLoadingStripe(false);
        }
    };

    const lowBalance = balance < 200;

    return (
        <div className="sp-card wallet-card-v2" id="wallet-card">
            <div className="sp-card-header">
                <span className="sp-card-label">
                    <span className="sp-card-icon">💳</span>
                    Wallet
                </span>
                {effectiveUserId && (
                    <span className="sp-user-id">{effectiveUserId}</span>
                )}
            </div>

            <div className="wallet-balance-row">
                <div className={`wallet-balance-amount ${lowBalance ? 'wallet-balance-low' : ''}`}>
                    ₹{displayBalance.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                </div>
                {lowBalance && <span className="wallet-low-badge">Low</span>}
            </div>

            <button
                className="wallet-topup-btn"
                id="wallet-topup-btn"
                onClick={handleTopUp}
                disabled={loadingStripe}
            >
                {loadingStripe ? (
                    <>
                        <span className="wallet-topup-spinner" />
                        Processing…
                    </>
                ) : (
                    <>
                        <span>＋</span>
                        Add ₹500
                    </>
                )}
            </button>

            {transactions.length > 0 && (
                <div className="wallet-tx-section">
                    <button
                        className="wallet-history-toggle"
                        id="wallet-history-toggle"
                        onClick={() => setShowHistory(!showHistory)}
                    >
                        <span className="wallet-history-arrow">{showHistory ? '▾' : '▸'}</span>
                        Transactions ({transactions.length})
                    </button>
                    {showHistory && (
                        <div className="wallet-tx-list">
                            {transactions.slice(-6).reverse().map((tx, i) => {
                                const { type, label } = parseTx(tx);
                                return (
                                    <div key={i} className={`wallet-tx-row tx-${type}`} id={`tx-${i}`}>
                                        <span className="tx-icon">{txIconMap[type]}</span>
                                        <span className="tx-label">{label}</span>
                                    </div>
                                );
                            })}
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}
