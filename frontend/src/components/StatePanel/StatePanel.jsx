import MenuSearch from './MenuSearch';
import SurgeBanner from './SurgeBanner';
import WalletCard from './WalletCard';
import InventoryTable from './InventoryTable';
import CouponDisplay from './CouponDisplay';
import ActiveOrders from './ActiveOrders';
import './StatePanel.css';

export default function StatePanel({ worldState, userId, onToggleSurge }) {
    if (!worldState) {
        return (
            <aside className="state-panel" id="state-panel">
                <div className="state-panel-loading">
                    <div className="state-spinner" />
                    <p>Loading world state...</p>
                </div>
            </aside>
        );
    }

    const wallet = worldState.wallets?.[userId];
    const restaurants = worldState.restaurants || [];
    const orders = worldState.active_orders || {};
    const surgeActive = worldState.surge_active || false;

    return (
        <aside className="state-panel" id="state-panel">
            <div className="state-panel-header">
                <span className="state-panel-title">🌐 Real-Time State</span>
                <span className="state-panel-dot" />
            </div>
            <div className="state-panel-content">
                <SurgeBanner active={surgeActive} onToggle={onToggleSurge} />
                {!surgeActive && onToggleSurge && (
                    <button
                        className="surge-toggle-btn"
                        id="surge-toggle-btn"
                        onClick={onToggleSurge}
                    >
                        ⛈️ Enable Surge Pricing
                    </button>
                )}
                <MenuSearch />
                <WalletCard wallet={wallet} />
                <ActiveOrders orders={orders} />
                <InventoryTable restaurants={restaurants} />
                <CouponDisplay restaurants={restaurants} />
            </div>
        </aside>
    );
}
