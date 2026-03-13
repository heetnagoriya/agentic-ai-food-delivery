import { useState, useCallback, useRef } from 'react';
import { searchMenu } from '../../utils/api';

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

        // Debounce: wait 400ms after user stops typing
        if (debounceRef.current) clearTimeout(debounceRef.current);
        debounceRef.current = setTimeout(() => doSearch(value), 400);
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        if (debounceRef.current) clearTimeout(debounceRef.current);
        doSearch(query);
    };

    const handleClear = () => {
        setQuery('');
        setResults([]);
        setHasSearched(false);
        if (debounceRef.current) clearTimeout(debounceRef.current);
    };

    return (
        <div className="menu-search" id="menu-search">
            <div className="section-header">
                <span>🔍 Menu Search</span>
            </div>
            <form className="menu-search-form" onSubmit={handleSubmit}>
                <div className="menu-search-input-wrapper">
                    <input
                        type="text"
                        className="menu-search-input"
                        id="menu-search-input"
                        value={query}
                        onChange={handleChange}
                        placeholder="Search food... (pizza, dosa, burger)"
                        autoComplete="off"
                    />
                    {query && (
                        <button
                            type="button"
                            className="menu-search-clear"
                            onClick={handleClear}
                            aria-label="Clear search"
                        >
                            ✕
                        </button>
                    )}
                </div>
            </form>

            {isSearching && (
                <div className="menu-search-loading">
                    <span className="menu-search-spinner" />
                    Searching...
                </div>
            )}

            {!isSearching && hasSearched && results.length === 0 && (
                <div className="menu-search-empty">
                    No items found for "{query}"
                </div>
            )}

            {results.length > 0 && (
                <div className="menu-search-results" id="menu-search-results">
                    {results.map((item, i) => (
                        <div key={i} className="menu-search-item" id={`search-result-${i}`}>
                            <div className="menu-search-item-top">
                                <span className="menu-search-item-name">{item.item}</span>
                                <span className="menu-search-item-price">₹{item.price}</span>
                            </div>
                            <div className="menu-search-item-bottom">
                                <span className="menu-search-item-restaurant">{item.restaurant}</span>
                                <span className="menu-search-item-rating">
                                    ★ {item.restaurant_rating} · {item.stock > 0 ? `${item.stock} left` : 'Out of stock'}
                                </span>
                            </div>
                            {item.surge_active && (
                                <span className="menu-search-surge-tag">⚡ Surge</span>
                            )}
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}
