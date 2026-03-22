const STATUS_STEPS = ['PLACED', 'PREPARING', 'OUT_FOR_DELIVERY', 'DELIVERED'];

const STATUS_META = {
    PLACED:            { emoji: '📋', label: 'Placed',       color: 'var(--text-muted)' },
    PREPARING:         { emoji: '👨‍🍳', label: 'Preparing',    color: '#f59e0b' },
    OUT_FOR_DELIVERY:  { emoji: '🛵', label: 'On the way',   color: '#22c55e' },
    DELIVERED:         { emoji: '✅', label: 'Delivered',    color: '#22c55e' },
    CANCELLED:         { emoji: '❌', label: 'Cancelled',    color: '#ef4444' },
};

function getStatusMeta(status) {
    return STATUS_META[status] || { emoji: '•', label: status, color: 'var(--text-muted)' };
}

function OrderProgressBar({ status }) {
    const statusIdx = STATUS_STEPS.indexOf(status);
    if (statusIdx < 0) return null;
    const progress = (statusIdx / (STATUS_STEPS.length - 1)) * 100;

    return (
        <div className="order-progress-container">
            <div className="order-progress-track">
                <div
                    className="order-progress-fill"
                    style={{ width: `${progress}%` }}
                />
            </div>
            <div className="order-progress-steps">
                {STATUS_STEPS.map((step, i) => (
                    <div
                        key={step}
                        className={`order-step-dot ${i <= statusIdx ? 'active' : ''}`}
                        title={step.replace(/_/g, ' ')}
                    />
                ))}
            </div>
        </div>
    );
}

function OrderCard({ order }) {
    const isCancelled = order.isCancelled || order.status === 'CANCELLED';
    const meta = isCancelled ? getStatusMeta('CANCELLED') : getStatusMeta(order.status);

    return (
        <div className={`sp-order-card ${isCancelled ? 'order-card-cancelled' : 'order-card-active'}`} id={`order-${order.orderId}`}>
            <div className="sp-order-top">
                <div className="sp-order-item">{order.item}</div>
                <span className="sp-order-amount">₹{order.paidAmount}</span>
            </div>

            <div className="sp-order-meta">
                <span className="sp-order-restaurant">{order.restaurantName || order.restaurant}</span>
                <span className="sp-order-id">{order.orderId?.slice(-8)}</span>
            </div>

            <div className="sp-order-status-row">
                <span className="sp-status-emoji">{meta.emoji}</span>
                <span className="sp-status-label" style={{ color: meta.color }}>
                    {meta.label}
                </span>
                {order.deliveryPartnerName && !isCancelled && (
                    <span className="sp-delivery-partner">· {order.deliveryPartnerName}</span>
                )}
            </div>

            {!isCancelled && order.status !== 'DELIVERED' && (
                <OrderProgressBar status={order.status} />
            )}
        </div>
    );
}

export default function ActiveOrders({ orders }) {
    const orderList = Object.values(orders || {});
    const activeCount = orderList.filter(o => !o.isCancelled && o.status !== 'DELIVERED').length;

    return (
        <div className="sp-card" id="active-orders">
            <div className="sp-card-header">
                <span className="sp-card-label">
                    <span className="sp-card-icon">🛵</span>
                    Active Orders
                </span>
                {activeCount > 0 && (
                    <span className="sp-count-badge">{activeCount}</span>
                )}
            </div>

            {orderList.length === 0 ? (
                <div className="sp-empty">
                    <span className="sp-empty-icon">📭</span>
                    <span>No active orders</span>
                </div>
            ) : (
                <div className="sp-order-list">
                    {orderList.map((order) => (
                        <OrderCard key={order.orderId} order={order} />
                    ))}
                </div>
            )}
        </div>
    );
}
