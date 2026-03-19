import { useState } from 'react';

export default function CouponDisplay({ restaurants }) {
    const [expandedRestaurants, setExpandedRestaurants] = useState(new Set());

    if (!restaurants || restaurants.length === 0) return null;

    const grouped = restaurants
        .filter((r) => r.coupons && r.coupons.length > 0)
        .map((r) => ({ name: r.name, coupons: r.coupons }));

    if (grouped.length === 0) return null;

    const allExpanded = grouped.length > 0 && expandedRestaurants.size === grouped.length;

    const toggleRestaurant = (name) => {
        setExpandedRestaurants((prev) => {
            const next = new Set(prev);
            if (next.has(name)) {
                next.delete(name);
            } else {
                next.add(name);
            }
            return next;
        });
    };

    const toggleAll = () => {
        if (allExpanded) {
            setExpandedRestaurants(new Set());
        } else {
            setExpandedRestaurants(new Set(grouped.map((g) => g.name)));
        }
    };

    return (
        <div className="coupon-display" id="coupon-display">
            <div className="section-header">
                <span>🎟️ Available Coupons</span>
                <button
                    className="inventory-expand-all-btn"
                    id="coupon-expand-all-btn"
                    onClick={toggleAll}
                    title={allExpanded ? 'Collapse All' : 'Expand All'}
                >
                    {allExpanded ? '▾ Collapse All' : '▸ Expand All'}
                </button>
            </div>
            <div className="coupon-groups">
                {grouped.map((group, i) => {
                    const isExpanded = expandedRestaurants.has(group.name);
                    return (
                        <div key={i} className="coupon-group">
                            <button
                                className="coupon-restaurant-header"
                                id={`coupon-toggle-${group.name.replace(/\s+/g, '-').toLowerCase()}`}
                                onClick={() => toggleRestaurant(group.name)}
                            >
                                <div className="coupon-restaurant-info">
                                    <span className="coupon-restaurant">{group.name}</span>
                                    <span className="coupon-count">{group.coupons.length} coupons</span>
                                </div>
                                <span className={`inventory-expand-icon ${isExpanded ? 'expanded' : ''}`}>
                                    ▸
                                </span>
                            </button>
                            {isExpanded && (
                                <div className="coupon-list coupon-list-animated">
                                    {group.coupons.map((c, j) => (
                                        <div key={j} className="coupon-badge" id={`coupon-${c.code}`}>
                                            <span className="coupon-code">{c.code}</span>
                                            <span className="coupon-desc">{c.description}</span>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    );
                })}
            </div>
        </div>
    );
}
