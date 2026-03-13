export default function CouponDisplay({ restaurants }) {
    if (!restaurants || restaurants.length === 0) return null;

    const grouped = restaurants
        .filter((r) => r.coupons && r.coupons.length > 0)
        .map((r) => ({ name: r.name, coupons: r.coupons }));

    if (grouped.length === 0) return null;

    return (
        <div className="coupon-display" id="coupon-display">
            <div className="section-header">
                <span>🎟️ Available Coupons</span>
            </div>
            <div className="coupon-groups">
                {grouped.map((group, i) => (
                    <div key={i} className="coupon-group">
                        <span className="coupon-restaurant">{group.name}</span>
                        <div className="coupon-list">
                            {group.coupons.map((c, j) => (
                                <div key={j} className="coupon-badge" id={`coupon-${c.code}`}>
                                    <span className="coupon-code">{c.code}</span>
                                    <span className="coupon-desc">{c.description}</span>
                                </div>
                            ))}
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}
