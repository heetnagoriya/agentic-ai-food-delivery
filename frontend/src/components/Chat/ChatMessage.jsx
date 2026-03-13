import { useState } from 'react';

function getStepColor(stepName) {
    const name = stepName.toLowerCase();
    if (name.includes('place_order') || name.includes('order')) return 'var(--accent-success)';
    if (name.includes('response') || name.includes('💬')) return 'var(--accent-primary)';
    return 'var(--text-secondary)';
}

function TraceStep({ step, index }) {
    const [expanded, setExpanded] = useState(false);

    return (
        <div
            className="inline-trace-step"
            style={{ animationDelay: `${index * 80}ms` }}
        >
            <div className="inline-trace-header" onClick={() => setExpanded(!expanded)}>
                <span className="inline-trace-dot" style={{ background: getStepColor(step.step) }} />
                <span className="inline-trace-name">{step.step}</span>
                <span className="inline-trace-duration">{step.durationMs}ms</span>
                <span className="inline-trace-chevron">{expanded ? '▾' : '▸'}</span>
            </div>
            {expanded && (
                <div className="inline-trace-body">
                    {step.input && (
                        <div className="inline-trace-detail">
                            <span className="inline-trace-label">Input</span>
                            <pre className="inline-trace-pre">{step.input}</pre>
                        </div>
                    )}
                    {step.output && (
                        <div className="inline-trace-detail">
                            <span className="inline-trace-label">Output</span>
                            <pre className="inline-trace-pre">{step.output}</pre>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}

function InlineTrace({ trace, isStreaming }) {
    const [open, setOpen] = useState(false);

    // Auto-open trace during streaming so user sees live progress
    const isOpen = open || isStreaming;

    if (!trace || trace.length === 0) return null;

    const totalMs = trace.reduce((sum, s) => sum + (s.durationMs || 0), 0);

    return (
        <div className="inline-trace">
            <button className="inline-trace-toggle" onClick={() => setOpen(!isOpen)}>
                <span className="inline-trace-toggle-icon">🧠</span>
                <span className="inline-trace-toggle-text">
                    {isOpen ? 'Hide' : 'View'} thinking
                    {isStreaming && ' (live)'}
                </span>
                <span className="inline-trace-toggle-meta">
                    {trace.length} step{trace.length !== 1 ? 's' : ''} · {totalMs}ms
                    {isStreaming && ' ⟳'}
                </span>
            </button>
            {isOpen && (
                <div className="inline-trace-steps">
                    {trace.map((step, i) => (
                        <TraceStep key={i} step={step} index={i} />
                    ))}
                </div>
            )}
        </div>
    );
}

export default function ChatMessage({ message }) {
    const isUser = message.role === 'user';
    const isStreaming = message.isStreaming && !message.text;

    return (
        <div
            className={`chat-message ${isUser ? 'chat-message-user' : 'chat-message-ai'} message-fade-in`}
            id={`message-${message.id}`}
        >
            {!isUser && <div className="message-avatar">🍽️</div>}
            <div className="message-body">
                {/* Show bubble only when there's text or we're not streaming */}
                {(message.text || !message.isStreaming) && (
                    <div className={`message-bubble ${isUser ? 'bubble-user' : 'bubble-ai'}`}>
                        <div className="message-content">{message.text || (isStreaming ? 'Processing...' : '')}</div>
                    </div>
                )}
                <div className="message-meta">
                    <span className="message-time">{message.time}</span>
                    {message.confidence != null && message.confidence > 0 && (
                        <span className="message-confidence">{message.confidence}% confidence</span>
                    )}
                </div>
                {!isUser && message.trace && (
                    <InlineTrace trace={message.trace} isStreaming={message.isStreaming} />
                )}
            </div>
        </div>
    );
}

