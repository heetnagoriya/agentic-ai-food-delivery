import { useState } from 'react';
import OrderHistoryModal from './OrderHistoryModal';
import UserSettingsModal from './UserSettingsModal';
import './Sidebar.css';

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
    onToggleTheme
}) {
    const [showHistoryModal, setShowHistoryModal] = useState(false);
    const [showSettingsModal, setShowSettingsModal] = useState(false);
    const orderList = orders ? Object.values(orders) : [];

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
                        <span className="logo-icon">🍽️</span>
                        <span className="logo-title">C.A.F.E.</span>
                    </div>
                    <button
                        className="sidebar-close-btn"
                        id="sidebar-close-btn"
                        onClick={onToggleCollapse}
                        aria-label="Close sidebar"
                    >
                        ✕
                    </button>
                </div>

                {/* Actions */}
                <div className="sidebar-actions">
                    <button className="new-chat-btn" id="btn-new-chat" onClick={onNewChat} style={{marginBottom: '10px'}}>
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                            <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
                        </svg>
                        New Chat
                    </button>
                    <button className="new-chat-btn" id="btn-order-history" onClick={() => setShowHistoryModal(true)} style={{background: 'var(--surface-light)', color: 'var(--text-primary)', marginBottom: '10px'}}>
                        📜 Full Order History
                    </button>
                    <button className="new-chat-btn" id="btn-settings" onClick={() => setShowSettingsModal(true)} style={{background: 'var(--surface-light)', color: 'var(--text-primary)'}}>
                        ⚙️ Settings
                    </button>
                </div>

                {/* Order History */}
                <div className="sidebar-section">
                    <span className="sidebar-section-label">Order History</span>
                    <div className="order-history-list" id="order-history-list">
                        {orderList.length === 0 ? (
                            <div className="order-history-empty">
                                <span className="order-history-empty-icon">📭</span>
                                <p>No orders yet</p>
                            </div>
                        ) : (
                            orderList.map((order) => (
                                <div
                                    key={order.orderId}
                                    className={`order-history-item ${order.isCancelled ? 'order-history-cancelled' : ''}`}
                                    id={`order-history-${order.orderId}`}
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
                        🧹 Reset Agent Memory
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
