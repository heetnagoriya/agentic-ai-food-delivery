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

export default function InventoryTable({ restaurants }) {
    if (!restaurants || restaurants.length === 0) {
        return (
            <div className="inventory-empty" id="inventory-table-empty">
                <span className="inventory-empty-icon">📦</span>
                <p>No inventory data available</p>
            </div>
        );
    }

    const rows = [];
    restaurants.forEach((r) => {
        (r.menu || []).forEach((item) => {
            rows.push({
                restaurant: r.name,
                isOpen: r.isOpen,
                rating: r.rating,
                item: item.name,
                price: item.price,
                stock: item.stockCount,
                tags: item.tags || [],
            });
        });
    });

    return (
        <div className="inventory-table-wrapper" id="inventory-table">
            <div className="section-header">
                <span>📦 Live Inventory</span>
                <Badge variant="default">{rows.length} items</Badge>
            </div>
            <div className="inventory-scroll">
                <table className="inventory-table">
                    <thead>
                        <tr>
                            <th>Restaurant</th>
                            <th>Item</th>
                            <th>Price</th>
                            <th>Stock</th>
                            <th>Status</th>
                            <th>Rating</th>
                        </tr>
                    </thead>
                    <tbody>
                        {rows.map((row, i) => {
                            const status = getStockStatus(row.stock);
                            return (
                                <tr key={i} className={i % 2 === 0 ? 'row-even' : 'row-odd'}>
                                    <td>
                                        <div className="restaurant-cell">
                                            <span className="restaurant-name">{row.restaurant}</span>
                                            <Badge variant={row.isOpen ? 'green' : 'red'}>
                                                {row.isOpen ? 'Open' : 'Closed'}
                                            </Badge>
                                        </div>
                                    </td>
                                    <td>
                                        <span className="item-name">{row.item}</span>
                                        <div className="item-tags">
                                            {row.tags.map((tag, j) => (
                                                <span key={j} className="item-tag">{tag}</span>
                                            ))}
                                        </div>
                                    </td>
                                    <td className="price-cell">₹{row.price}</td>
                                    <td className="stock-cell">{row.stock}</td>
                                    <td>
                                        <Badge variant={status.variant}>{status.label}</Badge>
                                    </td>
                                    <td>
                                        <StarRating rating={row.rating} />
                                    </td>
                                </tr>
                            );
                        })}
                    </tbody>
                </table>
            </div>
        </div>
    );
}
