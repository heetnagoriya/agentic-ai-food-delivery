import { useState } from 'react';

function getStepEmoji(stepName) {
    const name = stepName?.toLowerCase() || '';
    if (name.includes('search') || name.includes('find')) return '🔍';
    if (name.includes('rank') || name.includes('compare')) return '📊';
    if (name.includes('coupon') || name.includes('discount') || name.includes('apply')) return '🏷️';
    if (name.includes('price') || name.includes('calculate') || name.includes('cost')) return '🧮';
    if (name.includes('order') || name.includes('place') || name.includes('confirm')) return '✅';
    if (name.includes('wallet') || name.includes('balance') || name.includes('pay')) return '💳';
    if (name.includes('cancel')) return '❌';
    if (name.includes('track') || name.includes('delivery')) return '🚚';
    if (name.includes('response') || name.includes('reply') || name.includes('💬')) return '💬';
    if (name.includes('check') || name.includes('verify')) return '🔎';
    return '⚡';
}

export default function AgentTrace({ trace, isStreaming }) {
    const [isOpen, setIsOpen] = useState(false);

    if (!trace || trace.length === 0) return null;

    const shouldOpen = isOpen || isStreaming;
    const totalMs = trace.reduce((sum, s) => sum + (s.durationMs || 0), 0);
    const isComplete = !isStreaming;

    return (
        <div className={`trace-terminal ${shouldOpen ? 'open' : ''}`}>
            <div className="trace-terminal-header" onClick={() => setIsOpen(!isOpen)}>
                <div className="trace-header-left">
                    <span className={`trace-status-dot ${isComplete ? 'complete' : 'active'}`} />
                    <span className="trace-header-title">
                        {isStreaming ? 'Agent is thinking...' : 'Agent reasoning'}
                    </span>
                </div>
                <div className="trace-header-right">
                    <span className="trace-header-meta">
                        {trace.length} step{trace.length !== 1 ? 's' : ''} · {totalMs}ms
                    </span>
                    <span className="trace-header-arrow">▼</span>
                </div>
            </div>

            {shouldOpen && (
                <div className="trace-steps">
                    {trace.map((step, i) => {
                        const isLast = i === trace.length - 1;
                        const isActive = isStreaming && isLast;
                        const emoji = isActive ? '⏳' : getStepEmoji(step.step);

                        return (
                            <div
                                key={i}
                                className="trace-step-item"
                                style={{ animationDelay: `${i * 80}ms` }}
                            >
                                <span className="trace-step-icon">{emoji}</span>
                                <div className="trace-step-body">
                                    <div className="trace-step-name">
                                        {step.step}
                                        {step.durationMs > 0 && (
                                            <span className="trace-step-duration">{step.durationMs}ms</span>
                                        )}
                                        {isActive && (
                                            <span className="trace-active-dots">
                                                <span /><span /><span />
                                            </span>
                                        )}
                                    </div>
                                    {step.input && (
                                        <div className="trace-step-detail">
                                            → {step.input.substring(0, 80)}{step.input.length > 80 ? '...' : ''}
                                        </div>
                                    )}
                                </div>
                            </div>
                        );
                    })}
                </div>
            )}
        </div>
    );
}
