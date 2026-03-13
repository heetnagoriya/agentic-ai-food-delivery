import { useState } from 'react';

function getStepColor(stepName) {
    const name = stepName.toLowerCase();
    if (name.includes('place_order') || name.includes('order')) return 'var(--accent-emerald)';
    if (name.includes('response') || name.includes('💬')) return 'var(--accent-purple)';
    return 'var(--accent-cyan)';
}

function TraceStep({ step, index }) {
    const [expanded, setExpanded] = useState(false);
    const color = getStepColor(step.step);

    return (
        <div
            className="trace-step trace-step-slide-in"
            id={`trace-step-${index}`}
            style={{
                '--step-color': color,
                animationDelay: `${index * 100}ms`,
            }}
        >
            <div className="trace-step-header" onClick={() => setExpanded(!expanded)}>
                <span className="trace-step-indicator" />
                <span className="trace-step-name">{step.step}</span>
                <span className="trace-step-expand">{expanded ? '▾' : '▸'}</span>
                <span className="trace-step-duration">{step.durationMs}ms</span>
            </div>
            {expanded && (
                <div className="trace-step-body">
                    {step.input && (
                        <div className="trace-detail">
                            <span className="trace-detail-label">📥 Input</span>
                            <pre className="trace-detail-content">{step.input}</pre>
                        </div>
                    )}
                    {step.output && (
                        <div className="trace-detail">
                            <span className="trace-detail-label">📤 Output</span>
                            <pre className="trace-detail-content">{step.output}</pre>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}

export default function AgentTrace({ trace }) {
    const [open, setOpen] = useState(true);

    if (!trace || trace.length === 0) return null;

    const totalMs = trace.reduce((sum, s) => sum + (s.durationMs || 0), 0);

    return (
        <div className="agent-trace" id="agent-trace">
            <button
                className="trace-toggle"
                id="trace-toggle"
                onClick={() => setOpen(!open)}
            >
                <span className="trace-toggle-icon">🧠</span>
                <span className="trace-toggle-text">Agent Brain — Decision Trace</span>
                <span className="trace-toggle-meta">{trace.length} steps · {totalMs}ms total</span>
                <span className="trace-toggle-arrow">{open ? '▾' : '▸'}</span>
            </button>
            {open && (
                <div className="trace-steps">
                    {trace.map((step, i) => (
                        <TraceStep key={i} step={step} index={i} />
                    ))}
                </div>
            )}
        </div>
    );
}
