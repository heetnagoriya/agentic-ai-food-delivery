import { useEffect, useState } from 'react';
import { getOrderHistory } from '../../utils/api';
import '../common/Modal.css';

export default function OrderHistoryModal({ userId, onClose, onReorder }) {
    const [history, setHistory] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        getOrderHistory(userId)
            .then((data) => {
                setHistory(data);
                setLoading(false);
            })
            .catch((err) => {
                setError(err.message);
                setLoading(false);
            });
    }, [userId]);

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                <div className="modal-header">
                    <h2>📜 Full Order History</h2>
                    <button className="modal-close-btn" onClick={onClose}>✕</button>
                </div>
                <div className="modal-body">
                    {loading && <p>Loading history...</p>}
                    {error && <p style={{color: 'var(--accent)'}}>❌ {error}</p>}
                    {!loading && !error && history.length === 0 && (
                        <p style={{color: 'var(--text-secondary)'}}>No past orders found.</p>
                    )}
                    {!loading && !error && history.length > 0 && (
                        <div className="history-list">
                            {history.slice().reverse().map((order, idx) => (
                                <div key={idx} className="history-item-card">
                                    <div className="history-item-header">
                                        <span className="history-restaurant">{order.restaurantName}</span>
                                        <span className="history-price">₹{order.price}</span>
                                    </div>
                                    <div className="history-item-details">
                                        <div className="history-item-name">{order.itemName}</div>
                                        <div className="history-order-id">ID: {order.orderId}</div>
                                        {order.couponCode && (
                                            <div className="history-coupon">🎟️ {order.couponCode} applied</div>
                                        )}
                                    </div>
                                    <button 
                                        className="reorder-btn" 
                                        onClick={() => {
                                            onReorder(`reorder order ${order.orderId}`);
                                            onClose();
                                        }}
                                    >
                                        🔁 Reorder
                                    </button>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
