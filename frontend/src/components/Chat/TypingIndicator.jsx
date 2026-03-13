export default function TypingIndicator() {
    return (
        <div className="typing-indicator" id="typing-indicator">
            <div className="typing-avatar">🤖</div>
            <div className="typing-bubble">
                <span className="typing-dot" />
                <span className="typing-dot" />
                <span className="typing-dot" />
            </div>
        </div>
    );
}
