import { useState, useEffect, useRef } from 'react';
import { trackOrderAPI } from '../../utils/api';
import './DeliveryMap.css';

// Fallback label sequence for visual progression
const STAGE_LABELS = {
    PLACED: 'Order placed, waiting for restaurant...',
    PREPARING: 'Restaurant is preparing your food...',
    OUT_FOR_DELIVERY: 'Rider is on the way!',
    DELIVERED: 'Delivered! Enjoy your meal 🎉',
};

const STAGES = ['PLACED', 'PREPARING', 'OUT_FOR_DELIVERY', 'DELIVERED'];

function getStageIndex(status) {
    const idx = STAGES.indexOf(status);
    return idx >= 0 ? idx : 0;
}

export default function DeliveryMap({ order }) {
    const [trackingData, setTrackingData] = useState(null);
    const [expanded, setExpanded] = useState(false);
    const intervalRef = useRef(null);

    // Poll backend for live tracking data
    useEffect(() => {
        const fetchTracking = async () => {
            try {
                const data = await trackOrderAPI(order.orderId);
                setTrackingData(data);
            } catch (err) {
                console.error('Tracking fetch failed:', err);
            }
        };

        fetchTracking();
        intervalRef.current = setInterval(fetchTracking, 3000);
        return () => clearInterval(intervalRef.current);
    }, [order.orderId]);

    const status = trackingData?.status || order.status || 'PLACED';
    const eta = trackingData?.estimated_minutes_remaining ?? order.deliveryTimeMins ?? 5;
    const riderName = trackingData?.delivery_partner || order.deliveryPartnerName;
    const riderVehicle = trackingData?.delivery_vehicle || order.deliveryPartnerVehicle || '🏍️';
    const stageLabel = STAGE_LABELS[status] || status;
    const currentStageIdx = getStageIndex(status);

    // Compute progress from coordinates if available
    const coords = trackingData?.coordinates;
    let progress = 0;
    if (coords) {
        const rLat = coords.restaurant?.lat || 0;
        const rLng = coords.restaurant?.lng || 0;
        const uLat = coords.user?.lat || 0;
        const uLng = coords.user?.lng || 0;
        const dLat = coords.delivery_partner?.lat || rLat;
        const dLng = coords.delivery_partner?.lng || rLng;

        const totalDist = Math.sqrt((uLat - rLat) ** 2 + (uLng - rLng) ** 2);
        const riderDist = Math.sqrt((dLat - rLat) ** 2 + (dLng - rLng) ** 2);
        progress = totalDist > 0 ? Math.min(100, (riderDist / totalDist) * 100) : 0;
    }

    // Fallback progress from status
    if (!coords || progress === 0) {
        if (status === 'PLACED') progress = 5;
        else if (status === 'PREPARING') progress = 30;
        else if (status === 'OUT_FOR_DELIVERY') progress = 65;
        else if (status === 'DELIVERED') progress = 100;
    }

    // Timeline from backend or fallback
    const timeline = trackingData?.timeline || [
        { stage: 'PLACED', emoji: '📝', label: 'Order Placed', status: 'completed' },
        { stage: 'PREPARING', emoji: '👨‍🍳', label: 'Preparing', status: status === 'PREPARING' ? 'current' : (status === 'PLACED' ? 'pending' : 'completed') },
        { stage: 'OUT_FOR_DELIVERY', emoji: '🛵', label: 'On the way', status: status === 'OUT_FOR_DELIVERY' ? 'current' : (['PLACED', 'PREPARING'].includes(status) ? 'pending' : 'completed') },
        { stage: 'DELIVERED', emoji: '✅', label: 'Delivered', status: status === 'DELIVERED' ? 'current' : 'pending' },
    ];

    return (
        <>
            {/* Mini card */}
            <div className={`delivery-mini ${expanded ? 'delivery-mini-hidden' : ''}`} id="delivery-mini">
                <div className="delivery-mini-header">
                    <span className="delivery-mini-vehicle">{riderVehicle}</span>
                    <span className="delivery-mini-rider">{riderName}</span>
                    <span className="delivery-mini-eta">ETA {eta} min</span>
                    <button
                        className="delivery-expand-btn"
                        id="delivery-expand-btn"
                        onClick={() => setExpanded(true)}
                        aria-label="Expand map"
                    >
                        ⛶
                    </button>
                </div>
                <div className="delivery-mini-progress">
                    <div className="delivery-mini-bar" style={{ width: `${progress}%` }} />
                </div>
                <div className="delivery-mini-label">{stageLabel}</div>

                {/* Inline mini timeline with status dots */}
                <div className="delivery-mini-timeline">
                    {STAGES.map((stage, i) => {
                        const isDone = i < currentStageIdx;
                        const isCurrent = i === currentStageIdx;
                        return (
                            <div className="mini-timeline-step" key={stage}>
                                <div className={`mini-timeline-dot ${isDone ? 'dot-done' : ''} ${isCurrent ? 'dot-current' : ''}`} />
                                {i < STAGES.length - 1 && (
                                    <div className={`mini-timeline-line ${isDone ? 'line-done' : ''}`} />
                                )}
                            </div>
                        );
                    })}
                </div>
            </div>

            {/* Fullscreen overlay */}
            {expanded && (
                <div className="delivery-overlay" id="delivery-overlay">
                    <div className="delivery-full">
                        <div className="delivery-full-header">
                            <h3 className="delivery-full-title">
                                {riderVehicle} {riderName} is on the way
                            </h3>
                            <button
                                className="delivery-close-btn"
                                id="delivery-close-btn"
                                onClick={() => setExpanded(false)}
                            >
                                ✕
                            </button>
                        </div>

                        <div className="delivery-full-body">
                            {/* SVG Map */}
                            <div className="delivery-map-svg">
                                <svg viewBox="0 0 400 300" className="delivery-svg">
                                    {/* Road path */}
                                    <path
                                        d="M 40 260 Q 80 230 120 200 T 200 150 T 280 100 T 360 40"
                                        fill="none"
                                        stroke="#3f3f46"
                                        strokeWidth="3"
                                        strokeDasharray="8 4"
                                    />
                                    {/* Completed path */}
                                    <path
                                        d="M 40 260 Q 80 230 120 200 T 200 150 T 280 100 T 360 40"
                                        fill="none"
                                        stroke="#ea580c"
                                        strokeWidth="3"
                                        strokeDasharray={`${progress * 5} 1000`}
                                    />
                                    {/* Restaurant marker */}
                                    <circle cx="40" cy="260" r="8" fill="#ef4444" />
                                    <text x="55" y="265" fill="#a1a1aa" fontSize="10">Restaurant</text>
                                    {/* Destination marker */}
                                    <circle cx="360" cy="40" r="8" fill="#22c55e" />
                                    <text x="295" y="30" fill="#a1a1aa" fontSize="10">Your Location</text>
                                    {/* Rider dot */}
                                    <circle
                                        cx={40 + (320 * progress / 100)}
                                        cy={260 - (220 * progress / 100)}
                                        r="6"
                                        fill="#ea580c"
                                        className="rider-dot"
                                    />
                                    <circle
                                        cx={40 + (320 * progress / 100)}
                                        cy={260 - (220 * progress / 100)}
                                        r="12"
                                        fill="rgba(234, 88, 12, 0.25)"
                                        className="rider-pulse"
                                    />
                                </svg>
                            </div>

                            {/* Info panel */}
                            <div className="delivery-info-panel">
                                <div className="delivery-info-row">
                                    <span className="delivery-info-label">Order</span>
                                    <span className="delivery-info-value">{order.orderId}</span>
                                </div>
                                <div className="delivery-info-row">
                                    <span className="delivery-info-label">Item</span>
                                    <span className="delivery-info-value">{trackingData?.item || order.item}</span>
                                </div>
                                <div className="delivery-info-row">
                                    <span className="delivery-info-label">Restaurant</span>
                                    <span className="delivery-info-value">{trackingData?.restaurant || order.restaurantName}</span>
                                </div>
                                <div className="delivery-info-row">
                                    <span className="delivery-info-label">Rider</span>
                                    <span className="delivery-info-value">{riderVehicle} {riderName}</span>
                                </div>
                                <div className="delivery-info-row">
                                    <span className="delivery-info-label">Status</span>
                                    <span className="delivery-info-value">{status.replace(/_/g, ' ')}</span>
                                </div>
                                {coords?.delivery_partner && (
                                    <div className="delivery-info-row">
                                        <span className="delivery-info-label">Coordinates</span>
                                        <span className="delivery-info-value delivery-coords">
                                            {coords.delivery_partner.lat.toFixed(4)}, {coords.delivery_partner.lng.toFixed(4)}
                                        </span>
                                    </div>
                                )}
                                <div className="delivery-info-row delivery-eta-row">
                                    <span className="delivery-info-label">ETA</span>
                                    <span className="delivery-info-value delivery-eta-value">{eta} min</span>
                                </div>
                            </div>
                        </div>

                        {/* Route timeline */}
                        <div className="delivery-timeline">
                            {timeline.map((point, i) => (
                                <div key={i} className={`timeline-node ${point.status === 'completed' ? 'timeline-done' : ''} ${point.status === 'current' ? 'timeline-current' : ''}`}>
                                    <div className="timeline-dot">{point.emoji}</div>
                                    <span className="timeline-label">{point.label}</span>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>
            )}
        </>
    );
}
