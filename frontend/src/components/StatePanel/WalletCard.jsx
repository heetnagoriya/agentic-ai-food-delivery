import { useState, useEffect, useRef } from 'react';

export default function WalletCard({ wallet }) {
    const [showHistory, setShowHistory] = useState(false);
    const [displayBalance, setDisplayBalance] = useState(0);
    const prevBalanceRef = useRef(0);

    const balance = wallet?.balance ?? 0;
    const userId = wallet?.userId ?? '—';
    const transactions = wallet?.transactionHistory ?? [];

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

    function getTransactionIcon(tx) {
        if (tx.startsWith('DEBIT')) return { icon: '🔴', cls: 'tx-debit' };
        if (tx.startsWith('REFUND')) return { icon: '🟢', cls: 'tx-refund' };
        return { icon: '⚠️', cls: 'tx-failed' };
    }

    return (
        <div className="wallet-card" id="wallet-card">
            <div className="wallet-card-header">
                <span className="wallet-card-label">💳 Wallet Balance</span>
                <span className="wallet-card-user">{userId}</span>
            </div>
            <div className="wallet-card-balance">
                ₹{displayBalance.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
            </div>

            {transactions.length > 0 && (
                <>
                    <button
                        className="wallet-history-toggle"
                        id="wallet-history-toggle"
                        onClick={() => setShowHistory(!showHistory)}
                    >
                        {showHistory ? '▾' : '▸'} Transaction History ({transactions.length})
                    </button>
                    {showHistory && (
                        <div className="wallet-history">
                            {transactions.slice(-5).reverse().map((tx, i) => {
                                const { icon, cls } = getTransactionIcon(tx);
                                return (
                                    <div key={i} className={`wallet-tx ${cls}`} id={`wallet-tx-${i}`}>
                                        <span className="wallet-tx-icon">{icon}</span>
                                        <span className="wallet-tx-text">{tx}</span>
                                    </div>
                                );
                            })}
                        </div>
                    )}
                </>
            )}
        </div>
    );
}
