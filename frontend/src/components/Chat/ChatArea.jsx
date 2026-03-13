import { useRef, useEffect } from 'react';
import ChatMessage from './ChatMessage';
import ChatInput from './ChatInput';
import TypingIndicator from './TypingIndicator';
import './Chat.css';

export default function ChatArea({ messages, isLoading, onSend }) {
    const messagesEndRef = useRef(null);

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages, isLoading]);

    return (
        <div className="chat-area" id="chat-area">
            <div className="chat-messages" id="chat-messages">
                {messages.length === 0 && !isLoading && (
                    <div className="chat-empty-state" id="chat-empty-state">
                        <div className="empty-icon">🍽️</div>
                        <h3 className="empty-title">C.A.F.E. Agent</h3>
                        <p className="empty-subtitle">
                            I'm your autonomous food ordering agent. Tell me what you want
                            and I'll search menus, compare prices, find the best deals,
                            and place the order — all automatically.
                        </p>
                        <div className="empty-suggestions">
                            <span className="empty-chip" onClick={() => onSend('I want pizza')}>🍕 I want pizza</span>
                            <span className="empty-chip" onClick={() => onSend('Show me my wallet balance')}>💳 Wallet balance</span>
                            <span className="empty-chip" onClick={() => onSend('What are the best deals today?')}>🎟️ Best deals</span>
                            <span className="empty-chip" onClick={() => onSend('Order a dosa for me')}>🥘 Order dosa</span>
                        </div>
                    </div>
                )}

                {messages.map((msg) => (
                    <ChatMessage key={msg.id} message={msg} />
                ))}

                {isLoading && <TypingIndicator />}

                <div ref={messagesEndRef} />
            </div>

            <ChatInput onSend={onSend} isLoading={isLoading} />
        </div>
    );
}
