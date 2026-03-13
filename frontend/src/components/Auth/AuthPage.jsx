import { useState } from 'react';
import './Auth.css';

export default function AuthPage({ onLogin }) {
    const [mode, setMode] = useState('signin');
    const [userId, setUserId] = useState('');
    const [name, setName] = useState('');
    const [error, setError] = useState('');

    const handleSubmit = (e) => {
        e.preventDefault();
        setError('');

        const trimmedId = userId.trim();
        const trimmedName = name.trim();

        if (!trimmedId) {
            setError('Please enter a User ID');
            return;
        }
        if (!trimmedName) {
            setError('Please enter your name');
            return;
        }

        // Store in localStorage
        const userData = { userId: trimmedId, name: trimmedName };
        localStorage.setItem('cafe_user', JSON.stringify(userData));
        onLogin(userData);
    };

    return (
        <div className="auth-page" id="auth-page">
            <div className="auth-container">
                <div className="auth-logo">
                    <span className="auth-logo-icon">🍽️</span>
                    <h1 className="auth-logo-title">C.A.F.E.</h1>
                    <p className="auth-logo-subtitle">Controlled Autonomous Food Engine</p>
                </div>

                <div className="auth-card" id="auth-card">
                    <div className="auth-tabs">
                        <button
                            className={`auth-tab ${mode === 'signin' ? 'auth-tab-active' : ''}`}
                            id="auth-tab-signin"
                            onClick={() => { setMode('signin'); setError(''); }}
                            type="button"
                        >
                            Sign In
                        </button>
                        <button
                            className={`auth-tab ${mode === 'signup' ? 'auth-tab-active' : ''}`}
                            id="auth-tab-signup"
                            onClick={() => { setMode('signup'); setError(''); }}
                            type="button"
                        >
                            Sign Up
                        </button>
                    </div>

                    <form className="auth-form" onSubmit={handleSubmit}>
                        <div className="auth-field">
                            <label className="auth-label" htmlFor="auth-userid">User ID</label>
                            <input
                                type="text"
                                id="auth-userid"
                                className="auth-input"
                                placeholder="e.g., user_123"
                                value={userId}
                                onChange={(e) => setUserId(e.target.value)}
                                autoComplete="username"
                            />
                        </div>

                        <div className="auth-field">
                            <label className="auth-label" htmlFor="auth-name">Display Name</label>
                            <input
                                type="text"
                                id="auth-name"
                                className="auth-input"
                                placeholder="e.g., Heet"
                                value={name}
                                onChange={(e) => setName(e.target.value)}
                                autoComplete="name"
                            />
                        </div>

                        {error && <div className="auth-error" id="auth-error">{error}</div>}

                        <button type="submit" className="auth-submit" id="auth-submit">
                            {mode === 'signin' ? 'Sign In' : 'Create Account'}
                        </button>
                    </form>

                    <div className="auth-hint">
                        <p>Demo accounts: <code>user_123</code> (Heet) or <code>user_456</code> (Budget User)</p>
                    </div>
                </div>

                <p className="auth-footer">
                    AI-powered autonomous food ordering agent · Built with Google Gemini
                </p>
            </div>
        </div>
    );
}
