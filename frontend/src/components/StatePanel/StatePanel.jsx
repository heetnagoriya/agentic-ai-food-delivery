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
                <div className="state-panel-header">
                    <span className="state-panel-title">Live State</span>
                    <span className="state-panel-dot loading" />
                </div>
                <div className="state-panel-loading">
                    <div className="state-spinner" />
                    <p>Connecting to world state…</p>
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
                <div className="state-panel-title-row">
                    <span className="state-panel-title">Live State</span>
                    <span className="state-panel-dot" />
                </div>
                <SurgeBanner active={surgeActive} onToggle={onToggleSurge} />
            </div>
            <div className="state-panel-content">
                {/* Wallet must receive the logged-in userId for Add Money to work */}
                <WalletCard wallet={wallet} userId={userId} />
                <ActiveOrders orders={orders} />
                <MenuSearch />
                <InventoryTable restaurants={restaurants} />
                <CouponDisplay restaurants={restaurants} />
            </div>
        </aside>
    );
}
