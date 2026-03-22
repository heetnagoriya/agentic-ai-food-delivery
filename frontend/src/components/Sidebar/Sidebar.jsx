import { useState } from 'react';
import OrderHistoryModal from './OrderHistoryModal';
import UserSettingsModal from './UserSettingsModal';
import Logo from '../common/Logo';
import './Sidebar.css';

const AUTONOMY_LEVELS = [
    { key: 'full_auto', label: 'Full Auto' },
    { key: 'balanced', label: 'Balanced' },
    { key: 'conservative', label: 'Conservative' },
];

export default function Sidebar({
    userName,
    userId,
    onNewChat,
    onResetMemory,
    onLogout,
    orders,
    collapsed,
    onToggleCollapse,
    onSend,
    theme,
    onToggleTheme,
    onToggleAnalytics,
    onToggleWorldState,
    worldState,
    autonomyLevel = 'balanced',
    onAutonomyChange,
}) {
    const [showHistoryModal, setShowHistoryModal] = useState(false);
    const [showSettingsModal, setShowSettingsModal] = useState(false);
    const orderList = orders ? Object.values(orders) : [];

    const walletData = worldState?.wallets?.[userId];
    const walletBalance = walletData?.balance ?? (typeof walletData === 'number' ? walletData : 0);

    return (
        <>
            <button
                className="sidebar-toggle-mobile"
                id="sidebar-toggle-mobile"
                onClick={onToggleCollapse}
                aria-label="Toggle sidebar"
            >
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <line x1="3" y1="6" x2="21" y2="6" /><line x1="3" y1="12" x2="21" y2="12" /><line x1="3" y1="18" x2="21" y2="18" />
                </svg>
            </button>

            <aside className={`sidebar ${collapsed ? 'sidebar-collapsed' : ''}`} id="sidebar">
                {/* Header */}
                <div className="sidebar-header">
                    <div className="sidebar-logo">
                        <span className="logo-icon"><Logo size={20} /></span>
                        <span className="logo-title">C.A.F.E.</span>
                    </div>
                    <button
                        className="sidebar-close-btn"
                        id="sidebar-close-btn"
                        onClick={onToggleCollapse}
                        aria-label="Close sidebar"
                    >
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                            <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
                        </svg>
                    </button>
                </div>

                {/* Wallet Card */}
                <div className="sidebar-wallet" id="sidebar-wallet">
                    <div className="sidebar-wallet-header">
                        <span className="sidebar-wallet-label">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <rect x="2" y="5" width="20" height="14" rx="2" />
                                <line x1="2" y1="10" x2="22" y2="10" />
                            </svg>
                            Wallet
                        </span>
                    </div>
                    <div className="sidebar-wallet-balance">
                        ₹{walletBalance.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                    </div>
                </div>

                {/* Autonomy Segmented Control */}
                <div className="autonomy-control" id="autonomy-control">
                    <span className="autonomy-label">Agent Autonomy</span>
                    <div className="autonomy-segments">
                        {AUTONOMY_LEVELS.map((level) => (
                            <button
                                key={level.key}
                                className={`autonomy-segment ${autonomyLevel === level.key ? 'active' : ''}`}
                                id={`autonomy-${level.key}`}
                                onClick={() => onAutonomyChange?.(level.key)}
                            >
                                {level.label}
                            </button>
                        ))}
                    </div>
                </div>

                {/* Actions */}
                <div className="sidebar-actions">
                    <button className="sidebar-action-btn primary" id="btn-new-chat" onClick={onNewChat}>
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                            <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
                        </svg>
                        New Chat
                    </button>
                    <button className="sidebar-action-btn" id="btn-order-history" onClick={() => setShowHistoryModal(true)}>
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                            <path d="M12 8v4l3 3" /><circle cx="12" cy="12" r="10" />
                        </svg>
                        Order History
                    </button>
                    <button className="sidebar-action-btn" id="btn-world-state" onClick={onToggleWorldState}>
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                            <circle cx="12" cy="12" r="10" /><path d="M2 12h20" /><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z" />
                        </svg>
                        Live World State
                    </button>
                    <button className="sidebar-action-btn" id="btn-analytics" onClick={onToggleAnalytics}>
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                            <path d="M18 20V10" /><path d="M12 20V4" /><path d="M6 20v-6" />
                        </svg>
                        Analytics
                    </button>
                    <button className="sidebar-action-btn" id="btn-settings" onClick={() => setShowSettingsModal(true)}>
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                            <circle cx="12" cy="12" r="3" /><path d="M12 1v2M12 21v2M4.2 4.2l1.4 1.4M18.4 18.4l1.4 1.4M1 12h2M21 12h2M4.2 19.8l1.4-1.4M18.4 5.6l1.4-1.4" />
                        </svg>
                        Settings
                    </button>
                </div>

                {/* Recent Orders */}
                <div className="sidebar-section">
                    <span className="sidebar-section-label">Recent Orders</span>
                    <div className="order-history-list" id="order-history-list">
                        {orderList.length === 0 ? (
                            <div className="order-history-empty">
                                <span className="order-history-empty-icon">📭</span>
                                <p>No orders yet</p>
                            </div>
                        ) : (
                            orderList.map((order, i) => (
                                <div
                                    key={order.orderId}
                                    className={`order-history-item ${order.isCancelled ? 'order-history-cancelled' : ''}`}
                                    id={`order-history-${order.orderId}`}
                                    style={{ animationDelay: `${i * 50}ms` }}
                                >
                                    <div className="order-history-top">
                                        <span className="order-history-id">{order.orderId}</span>
                                        <span className={`order-history-status order-status-${order.status?.toLowerCase()}`}>
                                            {order.isCancelled ? 'Cancelled' : order.status?.replace(/_/g, ' ')}
                                        </span>
                                    </div>
                                    <div className="order-history-item-name">{order.item}</div>
                                    <div className="order-history-amount">₹{order.paidAmount}</div>
                                </div>
                            ))
                        )}
                    </div>
                </div>

                {/* Bottom */}
                <div className="sidebar-bottom">
                    <button className="sidebar-link-btn" id="btn-toggle-theme" onClick={onToggleTheme}>
                        {theme === 'dark' ? '☀️ Light Mode' : '🌙 Dark Mode'}
                    </button>
                    <button className="sidebar-link-btn" id="btn-reset-memory" onClick={onResetMemory}>
                        🧹 Reset Memory
                    </button>
                    <div className="sidebar-user-row">
                        <div className="sidebar-user-info">
                            <span className="sidebar-user-avatar">{userName?.charAt(0) || '?'}</span>
                            <div className="sidebar-user-details">
                                <span className="sidebar-user-name">{userName}</span>
                                <span className="sidebar-user-id">{userId}</span>
                            </div>
                        </div>
                        <button className="sidebar-logout-btn" id="btn-logout" onClick={onLogout} aria-label="Logout">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                                <polyline points="16 17 21 12 16 7" />
                                <line x1="21" y1="12" x2="9" y2="12" />
                            </svg>
                        </button>
                    </div>
                </div>
            </aside>

            {!collapsed && <div className="sidebar-overlay" onClick={onToggleCollapse} />}

            {showHistoryModal && (
                <OrderHistoryModal
                    userId={userId}
                    onClose={() => setShowHistoryModal(false)}
                    onReorder={onSend}
                />
            )}

            {showSettingsModal && (
                <UserSettingsModal
                    userId={userId}
                    onClose={() => setShowSettingsModal(false)}
                />
            )}
        </>
    );
}
