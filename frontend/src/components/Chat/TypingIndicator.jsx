import Logo from '../common/Logo';

export default function TypingIndicator() {
    return (
        <div className="typing-indicator" id="typing-indicator">
            <div className="typing-avatar" style={{ padding: 0, overflow: 'hidden' }}><Logo animate={true} size={24} /></div>
            <div className="typing-dots">
                <span className="typing-dot" />
                <span className="typing-dot" />
                <span className="typing-dot" />
            </div>
        </div>
    );
}
