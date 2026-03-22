/**
 * GenerativeUI — Bento-box style React components
 * rendered inline in the chat stream when the AI returns structured data.
 */

function RestaurantCard({ restaurant }) {
    const { name, rating, cuisine, deliveryTime, priceRange, image } = restaurant;

    const foodEmojis = {
        'pizza': '🍕', 'burger': '🍔', 'sushi': '🍣', 'indian': '🍛',
        'chinese': '🥡', 'thai': '🍜', 'mexican': '🌮', 'italian': '🍝',
        'dessert': '🍰', 'coffee': '☕', 'dosa': '🥘', 'biryani': '🍚',
    };

    const getEmoji = () => {
        if (image) return image;
        const key = Object.keys(foodEmojis).find(k =>
            name?.toLowerCase().includes(k) || cuisine?.toLowerCase().includes(k)
        );
        return foodEmojis[key] || '🍽️';
    };

    return (
        <div className="restaurant-card">
            <div className="restaurant-card-image">{getEmoji()}</div>
            <div className="restaurant-card-info">
                <div className="restaurant-card-name">{name}</div>
                <div className="restaurant-card-meta">
                    {rating && (
                        <span className="restaurant-rating">★ {rating}</span>
                    )}
                    {deliveryTime && <span>{deliveryTime}</span>}
                    {priceRange && <span>{priceRange}</span>}
                </div>
                {cuisine && (
                    <div className="restaurant-card-tags">
                        {cuisine.split(',').map((tag, i) => (
                            <span key={i} className="restaurant-tag">{tag.trim()}</span>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
}

function MenuItemCard({ item, onAddToCart }) {
    return (
        <div className="menu-item-card">
            <div className="menu-item-name">{item.name}</div>
            <div className="menu-item-price">₹{item.price}</div>
            {item.rating && (
                <div className="menu-item-rating">★ {item.rating}</div>
            )}
            {onAddToCart && (
                <button className="menu-item-btn" onClick={() => onAddToCart(item)}>
                    Add to Cart
                </button>
            )}
        </div>
    );
}

function OrderSummaryCard({ order }) {
    return (
        <div className="order-summary-card">
            <div className="order-summary-header">
                <span>✅</span>
                <span>Order Placed Successfully</span>
            </div>
            <div className="order-summary-details">
                {order.item && (
                    <div className="order-summary-row">
                        <span>Item</span>
                        <span>{order.item}</span>
                    </div>
                )}
                {order.restaurant && (
                    <div className="order-summary-row">
                        <span>Restaurant</span>
                        <span>{order.restaurant}</span>
                    </div>
                )}
                {order.orderId && (
                    <div className="order-summary-row">
                        <span>Order ID</span>
                        <span style={{ fontFamily: 'var(--font-mono)', fontSize: '0.75rem' }}>{order.orderId}</span>
                    </div>
                )}
                {order.paidAmount != null && (
                    <div className="order-summary-row order-summary-total">
                        <span>Total Paid</span>
                        <span>₹{order.paidAmount}</span>
                    </div>
                )}
            </div>
        </div>
    );
}

/**
 * Attempt to parse structured data from AI response text
 * and render Generative UI cards.
 */
export function tryParseGenerativeUI(text) {
    if (!text) return null;

    // Check for JSON blocks (```json ... ```) containing structured data
    const jsonBlockRegex = /```json\s*([\s\S]*?)```/g;
    const matches = [...text.matchAll(jsonBlockRegex)];

    const cards = [];
    for (const match of matches) {
        try {
            const data = JSON.parse(match[1]);
            if (Array.isArray(data)) {
                // Check if it's restaurants
                if (data[0]?.name && (data[0]?.rating || data[0]?.cuisine)) {
                    cards.push({ type: 'restaurants', data });
                }
                // Check if it's menu items
                else if (data[0]?.name && data[0]?.price != null) {
                    cards.push({ type: 'menu', data });
                }
            } else if (data.orderId || data.order_id) {
                cards.push({ type: 'order', data });
            }
        } catch {
            // Not valid JSON, skip
        }
    }

    return cards.length > 0 ? cards : null;
}

export default function GenerativeUI({ cards, onSend }) {
    if (!cards || cards.length === 0) return null;

    return (
        <div className="gen-ui-container">
            {cards.map((card, i) => {
                if (card.type === 'restaurants') {
                    return (
                        <div key={i}>
                            {card.data.map((r, j) => (
                                <RestaurantCard key={j} restaurant={r} />
                            ))}
                        </div>
                    );
                }
                if (card.type === 'menu') {
                    return (
                        <div key={i} className="menu-grid">
                            {card.data.map((item, j) => (
                                <MenuItemCard
                                    key={j}
                                    item={item}
                                    onAddToCart={(it) => onSend?.(`Order ${it.name}`)}
                                />
                            ))}
                        </div>
                    );
                }
                if (card.type === 'order') {
                    return <OrderSummaryCard key={i} order={card.data} />;
                }
                return null;
            })}
        </div>
    );
}
