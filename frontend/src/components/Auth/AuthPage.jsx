import { useState } from 'react';
import logoUrl from '../../assets/logo-transparent.png';
import { GoogleLogin } from '@react-oauth/google';
import { loginWithApi, registerWithApi, sendOtpApi, googleLoginApi } from '../../utils/api';
import './Auth.css';

export default function AuthPage({ onLogin }) {
    const [mode, setMode] = useState('signin');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [name, setName] = useState('');
    const [otp, setOtp] = useState('');
    const [otpSent, setOtpSent] = useState(false);
    
    const [error, setError] = useState('');
    const [successMsg, setSuccessMsg] = useState('');
    const [loading, setLoading] = useState(false);

    const handleGoogleSuccess = async (credentialResponse) => {
        try {
            setError('');
            setLoading(true);
            const data = await googleLoginApi(credentialResponse.credential);
            completeLogin(data, data.email || '', data.name || '');
        } catch (err) {
            setError(err.message || 'Google Authentication failed. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setSuccessMsg('');
        setLoading(true);

        const trimmedEmail = email.trim();
        const trimmedName = name.trim();
        const trimmedPassword = password.trim();
        const trimmedOtp = otp.trim();

        if (!trimmedEmail) { setError('Please enter your email address'); setLoading(false); return; }
        if (!trimmedPassword) { setError('Please enter your password'); setLoading(false); return; }
        
        try {
            if (mode === 'signin') {
                const data = await loginWithApi(trimmedEmail, trimmedPassword);
                completeLogin(data, trimmedEmail, '');
            } else {
                // SIGN UP MODE
                if (!trimmedName) { setError('Please enter your name'); setLoading(false); return; }

                if (!otpSent) {
                    // Step 1: Send OTP
                    await sendOtpApi(trimmedEmail);
                    setOtpSent(true);
                    setSuccessMsg(`Verification code sent to ${trimmedEmail}`);
                } else {
                    // Step 2: Verify & Register
                    if (!trimmedOtp) { setError('Please enter the 6-digit code'); setLoading(false); return; }
                    const data = await registerWithApi(trimmedName, trimmedEmail, trimmedPassword, trimmedOtp);
                    completeLogin(data, trimmedEmail, trimmedName);
                }
            }
        } catch (err) {
            setError(err.message || 'Authentication failed. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    const completeLogin = (data, email, name) => {
        const resolvedName = name || email.split('@')[0];
        const userData = { userId: data.userId, name: resolvedName };
        localStorage.setItem('cafe_user', JSON.stringify(userData));
        onLogin(userData);
    };

    const switchMode = (newMode) => {
        setMode(newMode);
        setError('');
        setSuccessMsg('');
        setOtpSent(false);
        setOtp('');
    };

    return (
        <div className="auth-page" id="auth-page">
            <div className="auth-container">
                {/* Left Hero Panel */}
                <div className="auth-hero">
                    <div className="auth-hero-icon">🤖</div>
                    <h1 className="auth-hero-title">Foodie-Bot</h1>
                    <p className="auth-hero-subtitle">
                        Your AI-powered food ordering copilot.<br />
                        Smart. Instant. Fully autonomous.
                    </p>
                    <div className="auth-features">
                        <div className="auth-feature-item">
                            <span>🧠</span>
                            <span>Remembers your past orders & preferences</span>
                        </div>
                        <div className="auth-feature-item">
                            <span>⚡</span>
                            <span>Real-time AI reasoning with live trace</span>
                        </div>
                        <div className="auth-feature-item">
                            <span>🎫</span>
                            <span>Auto-applies the best coupon for you</span>
                        </div>
                        <div className="auth-feature-item">
                            <span>🛡️</span>
                            <span>Allergy-safe filtering, always</span>
                        </div>
                    </div>
                </div>

                {/* Right Form Panel */}
                <div className="auth-form-panel">
                    <div className="auth-card" id="auth-card">
                        <div className="auth-brand">
                            <img src={logoUrl} alt="C.A.F.E. Logo" style={{height: '60px', marginBottom: '10px'}} />
                            <h2 className="auth-brand-name">C.A.F.E : The Agentic Food Delivery System.</h2>
                        </div>

                        <div className="auth-tabs">
                            <button
                                className={`auth-tab ${mode === 'signin' ? 'auth-tab-active' : ''}`}
                                onClick={() => switchMode('signin')}
                                type="button"
                            >Sign In</button>
                            <button
                                className={`auth-tab ${mode === 'signup' ? 'auth-tab-active' : ''}`}
                                onClick={() => switchMode('signup')}
                                type="button"
                            >Sign Up</button>
                        </div>

                        <div className="google-login-container" style={{ display: 'flex', justifyContent: 'center', marginBottom: '1.5rem', marginTop: '1rem' }}>
                            <GoogleLogin
                                onSuccess={handleGoogleSuccess}
                                onError={() => setError('Google Login Failed')}
                                theme="filled_black"
                                shape="pill"
                                text={mode === 'signin' ? 'signin_with' : 'signup_with'}
                            />
                        </div>

                        <div style={{ textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.8rem', letterSpacing: '0.05em', marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '10px', textTransform: 'uppercase' }}>
                            <span style={{flex: 1, height: '1px', background: 'var(--glass-border)'}}></span>
                            Or continue with email
                            <span style={{flex: 1, height: '1px', background: 'var(--glass-border)'}}></span>
                        </div>

                        <form className="auth-form" onSubmit={handleSubmit}>
                            {mode === 'signup' && (
                                <div className="auth-field">
                                    <label className="auth-label">Full Name</label>
                                    <input
                                        type="text"
                                        className="auth-input"
                                        placeholder="e.g., Heet Nagoriya"
                                        value={name}
                                        onChange={(e) => setName(e.target.value)}
                                        disabled={otpSent || loading}
                                        autoComplete="name"
                                    />
                                </div>
                            )}

                            <div className="auth-field">
                                <label className="auth-label">Email Address</label>
                                <input
                                    type="email"
                                    className="auth-input"
                                    placeholder="name@example.com"
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                    disabled={otpSent || loading}
                                    autoComplete="email"
                                />
                            </div>

                            <div className="auth-field">
                                <label className="auth-label">Password</label>
                                <input
                                    type="password"
                                    className="auth-input"
                                    placeholder="Enter password"
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    disabled={otpSent || loading}
                                    autoComplete={mode === 'signin' ? 'current-password' : 'new-password'}
                                />
                            </div>

                            {/* OTP Field (Only visible after sending code) */}
                            {mode === 'signup' && otpSent && (
                                <div className="auth-field" style={{ animation: 'fadeUp 0.3s ease' }}>
                                    <label className="auth-label" style={{ color: '#5bb98b' }}>Verification Code</label>
                                    <input
                                        type="text"
                                        className="auth-input"
                                        placeholder="6-digit code sent to email"
                                        value={otp}
                                        onChange={(e) => setOtp(e.target.value)}
                                        maxLength={6}
                                        disabled={loading}
                                        autoFocus
                                    />
                                </div>
                            )}

                            {error && <div className="auth-error">{error}</div>}
                            {successMsg && <div className="auth-error" style={{ background: 'rgba(91,185,139,0.1)', borderColor: 'rgba(91,185,139,0.3)', color: '#5bb98b' }}>{successMsg}</div>}

                            <button type="submit" className="auth-submit" disabled={loading}>
                                {loading ? '⏳ Please wait...' : 
                                  mode === 'signin' ? '→ Sign In' :
                                  (mode === 'signup' && !otpSent) ? '📧 Send Verification Code' :
                                  '🚀 Verify & Create Account'}
                            </button>
                        </form>

                        <div className="auth-hint">
                            <p>Secured with BCrypt & JWT</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
