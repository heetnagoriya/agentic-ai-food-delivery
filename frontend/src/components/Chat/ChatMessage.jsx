import { useState } from 'react';
import AgentTrace from './AgentTrace';
import GenerativeUI, { tryParseGenerativeUI } from './GenerativeUI';
import Logo from '../common/Logo';

function formatMessage(text) {
    if (!text) return '';
    // Strip JSON code blocks for clean display (Generative UI handles them)
    const cleaned = text.replace(/```json\s*[\s\S]*?```/g, '').trim();
    if (!cleaned) return null;
    return cleaned.split('\n').map((line, i) => {
        if (!line.trim()) return <br key={i} />;
        return <p key={i}>{line}</p>;
    });
}

export default function ChatMessage({ message, onSend }) {
    const isUser = message.role === 'user';
    const isStreaming = message.isStreaming && !message.text;

    // Try to extract generative UI cards from AI responses
    const genUICards = !isUser ? tryParseGenerativeUI(message.text) : null;
    const formattedText = formatMessage(message.text);

    return (
        <div className={`message-wrapper ${isUser ? 'user' : 'ai'}`} id={`message-${message.id}`}>
            <div className="message-row">

                {/* AI Avatar */}
                {!isUser && (
                    <div className="message-avatar ai-avatar" style={{ overflow: 'hidden' }}><Logo size={24} /></div>
                )}

                <div className="message-content">
                    {/* Trace Terminal (AI only, above response) */}
                    {!isUser && message.trace && message.trace.length > 0 && (
                        <AgentTrace trace={message.trace} isStreaming={message.isStreaming} />
                    )}

                    {/* Message Text */}
                    {(formattedText || isUser || isStreaming) && (
                        <div className="message-bubble">
                            {formattedText
                                ? formattedText
                                : (isStreaming
                                    ? <span style={{ color: 'var(--text-dim)' }}>Processing your request…</span>
                                    : ''
                                )
                            }
                        </div>
                    )}

                    {/* Generative UI Cards */}
                    {genUICards && (
                        <GenerativeUI cards={genUICards} onSend={onSend} />
                    )}

                    {/* Meta */}
                    {!isUser && !isStreaming && message.text && (
                        <div className="message-meta" style={{ justifyContent: 'flex-start' }}>
                            <span>{message.time}</span>
                            {message.confidence > 0 && (
                                <span className="confidence-chip">{message.confidence}% confidence</span>
                            )}
                        </div>
                    )}

                    {isUser && (
                        <div className="message-meta" style={{ justifyContent: 'flex-end' }}>
                            <span>{message.time}</span>
                        </div>
                    )}
                </div>

                {/* User Avatar */}
                {isUser && (
                    <div className="message-avatar user-avatar">👤</div>
                )}
            </div>
        </div>
    );
}
