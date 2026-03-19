import { useState } from 'react';
import Badge from '../common/Badge';

function getStockStatus(count) {
    if (count < 5) return { label: 'Critical', variant: 'red' };
    if (count < 15) return { label: 'Low', variant: 'amber' };
    return { label: 'OK', variant: 'green' };
}

function StarRating({ rating }) {
    const full = Math.floor(rating);
    const half = rating - full >= 0.5;
    const stars = [];
    for (let i = 0; i < full; i++) stars.push('★');
    if (half) stars.push('½');
    return <span className="star-rating">{stars.join('')} {rating.toFixed(1)}</span>;
}

function RestaurantSection({ restaurant, items, isExpanded, onToggle }) {
    return (
        <div className="inventory-restaurant-section">
            <button
                className="inventory-restaurant-header"
                onClick={onToggle}
                id={`inventory-toggle-${restaurant.name.replace(/\s+/g, '-').toLowerCase()}`}
            >
                <div className="inventory-restaurant-info">
                    <span className="inventory-restaurant-name">{restaurant.name}</span>
                    <div className="inventory-restaurant-meta">
                        <Badge variant={restaurant.isOpen ? 'green' : 'red'}>
                            {restaurant.isOpen ? 'Open' : 'Closed'}
                        </Badge>
                        <span className="inventory-item-count">{items.length} items</span>
                        <StarRating rating={restaurant.rating} />
                    </div>
                </div>
                <span className={`inventory-expand-icon ${isExpanded ? 'expanded' : ''}`}>
                    ▸
                </span>
            </button>
            {isExpanded && (
                <div className="inventory-restaurant-items">
                    <table className="inventory-table">
                        <thead>
                            <tr>
                                <th>Item</th>
                                <th>Price</th>
                                <th>Stock</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody>
                            {items.map((item, i) => {
                                const status = getStockStatus(item.stock);
                                return (
                                    <tr key={i} className={i % 2 === 0 ? 'row-even' : 'row-odd'}>
                                        <td>
                                            <span className="item-name">{item.name}</span>
                                            <div className="item-tags">
                                                {(item.tags || []).map((tag, j) => (
                                                    <span key={j} className="item-tag">{tag}</span>
                                                ))}
                                            </div>
                                        </td>
                                        <td className="price-cell">₹{item.price}</td>
                                        <td className="stock-cell">{item.stock}</td>
                                        <td>
                                            <Badge variant={status.variant}>{status.label}</Badge>
                                        </td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
}

export default function InventoryTable({ restaurants }) {
    const [expandedRestaurants, setExpandedRestaurants] = useState(new Set());

    if (!restaurants || restaurants.length === 0) {
        return (
            <div className="inventory-empty" id="inventory-table-empty">
                <span className="inventory-empty-icon">📦</span>
                <p>No inventory data available</p>
            </div>
        );
    }

    const totalItems = restaurants.reduce((sum, r) => sum + (r.menu || []).length, 0);
    const allExpanded = restaurants.length > 0 && expandedRestaurants.size === restaurants.length;

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
            setExpandedRestaurants(new Set(restaurants.map((r) => r.name)));
        }
    };

    return (
        <div className="inventory-table-wrapper" id="inventory-table">
            <div className="section-header">
                <span>📦 Live Inventory</span>
                <div className="inventory-header-actions">
                    <Badge variant="default">{totalItems} items</Badge>
                    <button
                        className="inventory-expand-all-btn"
                        id="inventory-expand-all-btn"
                        onClick={toggleAll}
                        title={allExpanded ? 'Collapse All' : 'Expand All'}
                    >
                        {allExpanded ? '▾ Collapse All' : '▸ Expand All'}
                    </button>
                </div>
            </div>
            <div className="inventory-sections">
                {restaurants.map((r) => (
                    <RestaurantSection
                        key={r.name}
                        restaurant={r}
                        items={r.menu || []}
                        isExpanded={expandedRestaurants.has(r.name)}
                        onToggle={() => toggleRestaurant(r.name)}
                    />
                ))}
            </div>
        </div>
    );
}
