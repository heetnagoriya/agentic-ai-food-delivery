import { useState, useRef, useEffect } from 'react';

export default function ChatInput({ onSend, isLoading }) {
    const [text, setText] = useState('');
    const textareaRef = useRef(null);

    const handleSubmit = (e) => {
        e.preventDefault();
        const trimmed = text.trim();
        if (!trimmed || isLoading) return;
        onSend(trimmed);
        setText('');
        // Reset textarea height
        if (textareaRef.current) {
            textareaRef.current.style.height = 'auto';
        }
    };

    const handleKeyDown = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSubmit(e);
        }
    };

    // Auto-resize textarea
    useEffect(() => {
        const textarea = textareaRef.current;
        if (!textarea) return;
        textarea.style.height = 'auto';
        const newHeight = Math.min(textarea.scrollHeight, 200);
        textarea.style.height = newHeight + 'px';
    }, [text]);

    return (
        <form className={`chat-input-form ${isLoading ? 'chat-input-loading' : ''}`} onSubmit={handleSubmit} id="chat-input-form">
            <div className="chat-input-wrapper">
                {/* Attachment button (decorative) */}
                <button type="button" className="chat-attach-btn" aria-label="Attach file" tabIndex={-1}>
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M21.44 11.05l-9.19 9.19a6 6 0 01-8.49-8.49l9.19-9.19a4 4 0 015.66 5.66l-9.2 9.19a2 2 0 01-2.83-2.83l8.49-8.48" />
                    </svg>
                </button>

                <textarea
                    ref={textareaRef}
                    className="chat-input"
                    id="chat-input"
                    value={text}
                    onChange={(e) => setText(e.target.value)}
                    onKeyDown={handleKeyDown}
                    placeholder="What would you like to order?"
                    disabled={isLoading}
                    autoComplete="off"
                    rows={1}
                />

                <button
                    type="submit"
                    className="chat-send-btn"
                    id="chat-send-btn"
                    disabled={isLoading || !text.trim()}
                    aria-label="Send message"
                >
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <line x1="12" y1="19" x2="12" y2="5" />
                        <polyline points="5 12 12 5 19 12" />
                    </svg>
                </button>
            </div>
        </form>
    );
}
