import { useState } from 'react';
import { completeOnboarding } from '../../utils/api';
import './OnboardingPage.css';

const CUISINE_OPTIONS = ['Indian', 'Italian', 'Chinese', 'Mexican', 'Fast Food', 'South Indian', 'Continental', 'Japanese'];
const FOOD_OPTIONS = ['Pizza', 'Biryani', 'Burger', 'Sushi', 'Pasta', 'Dosa', 'Noodles', 'Tacos', 'Salad', 'Sandwich'];
const ALLERGY_OPTIONS = ['Peanuts', 'Gluten', 'Dairy', 'Eggs', 'Shellfish', 'Soy', 'Tree Nuts', 'Fish'];

export default function OnboardingPage({ userId, onComplete }) {
  const [step, setStep] = useState(1);
  const [selectedCuisines, setSelectedCuisines] = useState([]);
  const [selectedFoods, setSelectedFoods] = useState([]);
  const [selectedAllergies, setSelectedAllergies] = useState([]);
  const [budgetMin, setBudgetMin] = useState(100);
  const [budgetMax, setBudgetMax] = useState(500);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const toggle = (arr, setArr, val) => {
    setArr(prev => prev.includes(val) ? prev.filter(v => v !== val) : [...prev, val]);
  };

  const handleSubmit = async () => {
    setLoading(true);
    setError('');
    try {
      await completeOnboarding({
        userId,
        favoriteFoods: selectedFoods,
        cuisines: selectedCuisines,
        allergies: selectedAllergies,
        budgetMin: Number(budgetMin),
        budgetMax: Number(budgetMax),
      });
      onComplete();
    } catch (e) {
      setError(e.message || 'Failed to save preferences');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="onboarding-backdrop">
      <div className="onboarding-card">
        {/* Header */}
        <div className="onboarding-header">
          <div className="onboarding-logo">🍽️</div>
          <h1>Welcome to Foodie-Bot!</h1>
          <p>Let's personalize your experience. Step {step} of 3.</p>
          <div className="onboarding-progress">
            {[1, 2, 3].map(s => (
              <div key={s} className={`onboarding-pip ${step >= s ? 'active' : ''}`} />
            ))}
          </div>
        </div>

        {/* Step 1: Cuisines */}
        {step === 1 && (
          <div className="onboarding-step">
            <h2>🍛 What cuisines do you love?</h2>
            <p>Pick everything that sounds delicious to you.</p>
            <div className="chip-grid">
              {CUISINE_OPTIONS.map(c => (
                <button
                  key={c}
                  className={`chip ${selectedCuisines.includes(c) ? 'selected' : ''}`}
                  onClick={() => toggle(selectedCuisines, setSelectedCuisines, c)}
                >
                  {c}
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Step 2: Foods + Allergies */}
        {step === 2 && (
          <div className="onboarding-step">
            <h2>🍕 Favourite dishes?</h2>
            <p>This helps the AI recommend your go-to meals.</p>
            <div className="chip-grid">
              {FOOD_OPTIONS.map(f => (
                <button
                  key={f}
                  className={`chip ${selectedFoods.includes(f) ? 'selected' : ''}`}
                  onClick={() => toggle(selectedFoods, setSelectedFoods, f)}
                >
                  {f}
                </button>
              ))}
            </div>

            <h2 style={{marginTop: '1.5rem'}}>⚠️ Any allergies?</h2>
            <p>The AI will <strong>never</strong> suggest dishes with these ingredients.</p>
            <div className="chip-grid">
              {ALLERGY_OPTIONS.map(a => (
                <button
                  key={a}
                  className={`chip allergy ${selectedAllergies.includes(a) ? 'selected-danger' : ''}`}
                  onClick={() => toggle(selectedAllergies, setSelectedAllergies, a)}
                >
                  {a}
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Step 3: Budget */}
        {step === 3 && (
          <div className="onboarding-step">
            <h2>💰 What's your budget per meal?</h2>
            <p>This is used to filter recommendations and flag expensive orders.</p>
            <div className="budget-row">
              <div className="budget-input-group">
                <label>Min (₹)</label>
                <input
                  type="number"
                  min={0} max={budgetMax}
                  value={budgetMin}
                  onChange={e => setBudgetMin(e.target.value)}
                />
              </div>
              <div className="budget-separator">to</div>
              <div className="budget-input-group">
                <label>Max (₹)</label>
                <input
                  type="number"
                  min={budgetMin}
                  value={budgetMax}
                  onChange={e => setBudgetMax(e.target.value)}
                />
              </div>
            </div>
            <div className="budget-preview">
              Budget set: ₹{budgetMin} – ₹{budgetMax} per order
            </div>
            {error && <p className="onboarding-error">{error}</p>}
          </div>
        )}

        {/* Navigation */}
        <div className="onboarding-nav">
          {step > 1 && (
            <button className="btn-secondary" onClick={() => setStep(s => s - 1)}>
              ← Back
            </button>
          )}
          {step < 3 ? (
            <button className="btn-primary" onClick={() => setStep(s => s + 1)}>
              Continue →
            </button>
          ) : (
            <button className="btn-primary" onClick={handleSubmit} disabled={loading}>
              {loading ? 'Saving…' : '🚀 Get Started!'}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
