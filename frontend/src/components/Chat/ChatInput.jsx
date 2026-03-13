import { useState } from 'react';

export default function ChatInput({ onSend, isLoading }) {
    const [text, setText] = useState('');

    const handleSubmit = (e) => {
        e.preventDefault();
        const trimmed = text.trim();
        if (!trimmed || isLoading) return;
        onSend(trimmed);
        setText('');
    };

    const handleKeyDown = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSubmit(e);
        }
    };

    return (
        <form className={`chat-input-form ${isLoading ? 'chat-input-loading' : ''}`} onSubmit={handleSubmit} id="chat-input-form">
            <div className="chat-input-wrapper">
                <input
                    type="text"
                    className="chat-input"
                    id="chat-input"
                    value={text}
                    onChange={(e) => setText(e.target.value)}
                    onKeyDown={handleKeyDown}
                    placeholder="Tell C.A.F.E. what you'd like... (e.g., 'Order me a pizza')"
                    disabled={isLoading}
                    autoComplete="off"
                />
                <button
                    type="submit"
                    className="chat-send-btn"
                    id="chat-send-btn"
                    disabled={isLoading || !text.trim()}
                    aria-label="Send message"
                >
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <line x1="22" y1="2" x2="11" y2="13" />
                        <polygon points="22 2 15 22 11 13 2 9 22 2" />
                    </svg>
                </button>
            </div>
        </form>
    );
}
