export default function SurgeBanner({ active, onToggle }) {
    return (
        <button
            className={`surge-pill ${active ? 'surge-pill-active' : 'surge-pill-idle'}`}
            id="surge-toggle-btn"
            onClick={onToggle}
            title={active ? 'Disable surge pricing' : 'Enable surge pricing'}
        >
            {active ? '⚡ Surge ON' : '☀️ Normal'}
        </button>
    );
}
