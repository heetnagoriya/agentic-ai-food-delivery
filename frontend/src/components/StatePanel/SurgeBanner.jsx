export default function SurgeBanner({ active, onToggle }) {
    if (!active) return null;
    return (
        <div className="surge-banner" id="surge-banner">
            <div className="surge-banner-content">
                <span className="surge-icon">⛈️</span>
                <span className="surge-text">SURGE PRICING ACTIVE — All prices 1.5x</span>
            </div>
            {onToggle && (
                <button className="surge-disable-btn" id="surge-disable-btn" onClick={onToggle}>
                    Disable
                </button>
            )}
        </div>
    );
}
