import { useState, useCallback, useRef } from 'react';
import { searchMenu } from '../../utils/api';

const QUICK_SEARCHES = ['pizza', 'burger', 'dosa', 'biryani'];

function getCuisineEmoji(cuisine) {
    const map = { Italian: '🍕', 'Fast Food': '🍔', 'South Indian': '🥘', Indian: '🍛' };
    return map[cuisine] || '🍽️';
}

export default function MenuSearch() {
    const [query, setQuery] = useState('');
    const [results, setResults] = useState([]);
    const [isSearching, setIsSearching] = useState(false);
    const [hasSearched, setHasSearched] = useState(false);
    const debounceRef = useRef(null);

    const doSearch = useCallback(async (q) => {
        if (!q.trim()) {
            setResults([]);
            setHasSearched(false);
            return;
        }
        setIsSearching(true);
        try {
            const data = await searchMenu(q.trim());
            setResults(data);
            setHasSearched(true);
        } catch (err) {
            console.error('Menu search failed:', err);
            setResults([]);
        } finally {
            setIsSearching(false);
        }
    }, []);

    const handleChange = (e) => {
        const value = e.target.value;
        setQuery(value);
        if (debounceRef.current) clearTimeout(debounceRef.current);
        debounceRef.current = setTimeout(() => doSearch(value), 350);
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        if (debounceRef.current) clearTimeout(debounceRef.current);
        doSearch(query);
    };

    const handleQuickSearch = (term) => {
        setQuery(term);
        if (debounceRef.current) clearTimeout(debounceRef.current);
        doSearch(term);
    };

    const handleClear = () => {
        setQuery('');
        setResults([]);
        setHasSearched(false);
        if (debounceRef.current) clearTimeout(debounceRef.current);
    };

    return (
        <div className="sp-card" id="menu-search">
            <div className="sp-card-header">
                <span className="sp-card-label">
                    <span className="sp-card-icon">🔍</span>
                    Menu Search
                </span>
            </div>

            <form className="ms-form" onSubmit={handleSubmit} id="menu-search-form">
                <div className="ms-input-wrapper">
                    <span className="ms-search-icon">⌕</span>
                    <input
                        type="text"
                        className="ms-input"
                        id="menu-search-input"
                        value={query}
                        onChange={handleChange}
                        placeholder="pizza, dosa, burger…"
                        autoComplete="off"
                    />
                    {query && (
                        <button type="button" className="ms-clear-btn" onClick={handleClear} aria-label="Clear">
                            ✕
                        </button>
                    )}
                </div>
            </form>

            {/* Quick search chips */}
            {!hasSearched && !query && (
                <div className="ms-quick-chips">
                    {QUICK_SEARCHES.map((term) => (
                        <button
                            key={term}
                            className="ms-chip"
                            onClick={() => handleQuickSearch(term)}
                        >
                            {term}
                        </button>
                    ))}
                </div>
            )}

            {isSearching && (
                <div className="ms-loading">
                    <span className="ms-spinner" />
                    Searching…
                </div>
            )}

            {!isSearching && hasSearched && results.length === 0 && (
                <div className="ms-empty">No results for "{query}"</div>
            )}

            {results.length > 0 && (
                <div className="ms-results" id="menu-search-results">
                    {results.map((item, i) => (
                        <div key={i} className="ms-result-item" id={`search-result-${i}`}>
                            <span className="ms-result-emoji">{getCuisineEmoji(item.cuisine)}</span>
                            <div className="ms-result-info">
                                <div className="ms-result-top">
                                    <span className="ms-result-name">{item.item}</span>
                                    <span className="ms-result-price">
                                        ₹{item.price}
                                        {item.surge_active && <span className="ms-surge-tag"> ⚡</span>}
                                    </span>
                                </div>
                                <div className="ms-result-meta">
                                    <span className="ms-result-restaurant">{item.restaurant}</span>
                                    <span className="ms-result-rating">★ {item.restaurant_rating}</span>
                                </div>
                                {item.allergy_warning && (
                                    <div className="ms-allergy-warn">⚠️ {item.allergy_warning}</div>
                                )}
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}
