import { useRef, useEffect } from 'react';
import ChatMessage from './ChatMessage';
import ChatInput from './ChatInput';
import TypingIndicator from './TypingIndicator';
import Logo from '../common/Logo';
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
                        <div className="empty-logo" style={{ marginBottom: "1rem" }}><Logo animate={true} size={64} /></div>
                        <h1 className="empty-title">What can I get you?</h1>
                        <p className="empty-subtitle">
                            I'm your autonomous food agent. Tell me what you're craving 
                            and I'll search menus, find the best deals, and place your order — all automatically.
                        </p>
                        <div className="empty-suggestions">
                            <button className="empty-chip" onClick={() => onSend('I want pizza')}>
                                <span className="empty-chip-icon">🍕</span>
                                I want pizza
                            </button>
                            <button className="empty-chip" onClick={() => onSend('Show me my wallet balance')}>
                                <span className="empty-chip-icon">💳</span>
                                Wallet balance
                            </button>
                            <button className="empty-chip" onClick={() => onSend('What are the best deals today?')}>
                                <span className="empty-chip-icon">🏷️</span>
                                Best deals today
                            </button>
                            <button className="empty-chip" onClick={() => onSend('Order a dosa for me')}>
                                <span className="empty-chip-icon">🥘</span>
                                Order a dosa
                            </button>
                        </div>
                    </div>
                )}

                {messages.map((msg) => (
                    <ChatMessage key={msg.id} message={msg} onSend={onSend} />
                ))}

                {isLoading && <TypingIndicator />}

                <div ref={messagesEndRef} />
            </div>

            <ChatInput onSend={onSend} isLoading={isLoading} />
        </div>
    );
}
