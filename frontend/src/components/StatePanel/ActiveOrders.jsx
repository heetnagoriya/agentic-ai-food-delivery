const STATUS_STEPS = ['PLACED', 'PREPARING', 'OUT_FOR_DELIVERY', 'DELIVERED'];

function getStatusIndex(status) {
    const idx = STATUS_STEPS.indexOf(status);
    return idx >= 0 ? idx : 0;
}

function OrderCard({ order }) {
    const statusIdx = getStatusIndex(order.status);
    const isCancelled = order.isCancelled;

    return (
        <div className={`order-card ${isCancelled ? 'order-cancelled' : ''}`} id={`order-${order.orderId}`}>
            <div className="order-card-header">
                <span className="order-id">{order.orderId}</span>
                {isCancelled ? (
                    <span className="order-status-cancelled">CANCELLED</span>
                ) : (
                    <span className="order-status-badge status-shimmer">{order.status}</span>
                )}
            </div>
            <div className="order-item-name">{order.item}</div>
            <div className="order-details">
                <span className="order-detail">
                    {order.deliveryPartnerVehicle} {order.deliveryPartnerName}
                </span>
                <span className="order-amount">₹{order.paidAmount}</span>
            </div>

            {!isCancelled && (
                <div className="order-progress">
                    <div className="progress-track">
                        {STATUS_STEPS.map((step, i) => (
                            <div key={step} className="progress-node-wrapper">
                                <div className={`progress-node ${i <= statusIdx ? 'progress-node-active' : ''}`} />
                                {i < STATUS_STEPS.length - 1 && (
                                    <div className={`progress-line ${i < statusIdx ? 'progress-line-active' : ''}`} />
                                )}
                            </div>
                        ))}
                    </div>
                    <div className="progress-labels">
                        {STATUS_STEPS.map((step) => (
                            <span key={step} className="progress-label">{step.replace(/_/g, ' ')}</span>
                        ))}
                    </div>
                </div>
            )}
        </div>
    );
}

export default function ActiveOrders({ orders }) {
    if (!orders || Object.keys(orders).length === 0) {
        return (
            <div className="orders-empty" id="active-orders-empty">
                <div className="section-header"><span>🛵 Active Orders</span></div>
                <div className="empty-mini">
                    <span className="empty-mini-icon">📭</span>
                    <p>No active orders</p>
                </div>
            </div>
        );
    }

    return (
        <div className="active-orders" id="active-orders">
            <div className="section-header">
                <span>🛵 Active Orders</span>
                <span className="order-count">{Object.keys(orders).length}</span>
            </div>
            <div className="order-list">
                {Object.values(orders).map((order) => (
                    <OrderCard key={order.orderId} order={order} />
                ))}
            </div>
        </div>
    );
}
