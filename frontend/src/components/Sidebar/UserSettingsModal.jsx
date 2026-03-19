import { useEffect, useState } from 'react';
import { getUserProfile, updateUserProfile } from '../../utils/api';
import '../common/Modal.css';

export default function UserSettingsModal({ userId, onClose }) {
    const [profile, setProfile] = useState(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    
    // Form fields
    const [language, setLanguage] = useState('');
    const [allergies, setAllergies] = useState('');
    const [autonomy, setAutonomy] = useState('');

    useEffect(() => {
        getUserProfile(userId).then(p => {
            setProfile(p);
            setLanguage(p.languagePreference || 'English');
            setAllergies((p.preferences?.allergies || []).join(', '));
            setAutonomy(p.autonomyLevel || 'BALANCED');
            setLoading(false);
        }).catch(err => {
            console.error(err);
            setLoading(false);
        });
    }, [userId]);

    const handleSave = async () => {
        setSaving(true);
        const allergiesList = allergies.split(',').map(s => s.trim()).filter(Boolean);
        try {
            await updateUserProfile(userId, {
                languagePreference: language,
                allergies: allergiesList,
                autonomyLevel: autonomy
            });
            onClose();
        } catch (err) {
            alert('Failed to save settings: ' + err.message);
            setSaving(false);
        }
    };

    if (loading) return null;

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-content" onClick={e => e.stopPropagation()}>
                <div className="modal-header">
                    <h2>⚙️ User Settings</h2>
                    <button className="modal-close-btn" onClick={onClose}>✕</button>
                </div>
                <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                    <div className="form-group">
                        <label style={{fontWeight: 600, fontSize: '0.9rem', color: 'var(--text-secondary)'}}>Preferred Language</label>
                        <select 
                            value={language} 
                            onChange={e => setLanguage(e.target.value)} 
                            style={{width:'100%', padding:'10px', marginTop:'6px', borderRadius:'6px', background:'var(--surface-light)', color:'var(--text-primary)', border:'1px solid var(--border)'}}
                        >
                            <option value="English">English</option>
                            <option value="Hindi">Hindi</option>
                            <option value="Gujarati">Gujarati</option>
                        </select>
                    </div>
                    <div className="form-group">
                        <label style={{fontWeight: 600, fontSize: '0.9rem', color: 'var(--text-secondary)'}}>Allergies (comma-separated)</label>
                        <input 
                            type="text" 
                            value={allergies} 
                            onChange={e => setAllergies(e.target.value)} 
                            placeholder="e.g. Peanuts, Gluten"
                            style={{width:'100%', padding:'10px', marginTop:'6px', borderRadius:'6px', background:'var(--surface-light)', color:'var(--text-primary)', border:'1px solid var(--border)'}}
                        />
                    </div>
                    <div className="form-group">
                        <label style={{fontWeight: 600, fontSize: '0.9rem', color: 'var(--text-secondary)'}}>Autonomy Level (AI Behavior)</label>
                        <select 
                            value={autonomy} 
                            onChange={e => setAutonomy(e.target.value)} 
                            style={{width:'100%', padding:'10px', marginTop:'6px', borderRadius:'6px', background:'var(--surface-light)', color:'var(--text-primary)', border:'1px solid var(--border)'}}
                        >
                            <option value="FULL_AUTO">Full Auto (No confirmation needed)</option>
                            <option value="BALANCED">Balanced (Confirm if unsure)</option>
                            <option value="CONSERVATIVE">Conservative (Always confirm)</option>
                        </select>
                    </div>
                    <button 
                        className="reorder-btn" 
                        onClick={handleSave} 
                        disabled={saving}
                        style={{marginTop: '16px', padding: '12px'}}
                    >
                        {saving ? 'Saving...' : 'Save Settings'}
                    </button>
                </div>
            </div>
        </div>
    );
}
