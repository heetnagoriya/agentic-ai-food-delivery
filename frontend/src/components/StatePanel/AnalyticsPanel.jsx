import { useState, useEffect, useMemo } from 'react';
import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid, PieChart, Pie, Cell } from 'recharts';
import { getOrderHistory } from '../../utils/api';
import './AnalyticsPanel.css';

const COLORS = ['#f97316', '#ec4899', '#8b5cf6', '#3b82f6', '#10b981'];

export default function AnalyticsPanel({ userId }) {
    const [orders, setOrders] = useState([]);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        if (!userId) return;
        setIsLoading(true);
        getOrderHistory(userId)
            .then(data => setOrders(data || []))
            .catch(err => console.error("Failed to load analytics data:", err))
            .finally(() => setIsLoading(false));
    }, [userId]);

    const stats = useMemo(() => {
        if (!orders.length) return { totalSpent: 0, orderCount: 0, avgValue: 0, topRest: '—' };

        const totalSpent = orders.reduce((sum, o) => sum + (o.totalPrice || 0), 0);
        const orderCount = orders.length;
        const avgValue = totalSpent / orderCount;

        const restCounts = {};
        orders.forEach(o => {
            if (o.restaurantId) {
                restCounts[o.restaurantId] = (restCounts[o.restaurantId] || 0) + 1;
            }
        });
        const topRest = Object.entries(restCounts).sort((a, b) => b[1] - a[1])[0]?.[0] || '—';

        return { totalSpent, orderCount, avgValue, topRest };
    }, [orders]);

    const lineData = useMemo(() => {
        // Group by date
        const grouped = {};
        [...orders].reverse().forEach(o => {
            if (!o.timestamp) return;
            const date = new Date(o.timestamp).toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
            grouped[date] = (grouped[date] || 0) + (o.totalPrice || 0);
        });
        return Object.entries(grouped).map(([date, amount]) => ({ date, amount }));
    }, [orders]);

    const pieData = useMemo(() => {
        const counts = {};
        orders.forEach(o => {
            if (o.restaurantId) {
                counts[o.restaurantId] = (counts[o.restaurantId] || 0) + 1;
            }
        });
        return Object.entries(counts).map(([name, value]) => ({ name, value }));
    }, [orders]);

    if (isLoading) {
        return (
            <aside className="analytics-panel">
                <div className="analytics-loading">
                    <div className="state-spinner" />
                    <p>Loading analytics...</p>
                </div>
            </aside>
        );
    }

    return (
        <aside className="analytics-panel glass-panel">
            <div className="analytics-header">
                <h2 className="analytics-title">📊 Spending Analytics</h2>
            </div>
            
            <div className="analytics-content">
                <div className="stats-grid">
                    <div className="stat-card">
                        <span className="stat-label">Total Spent</span>
                        <span className="stat-value text-gradient">₹{stats.totalSpent.toFixed(2)}</span>
                    </div>
                    <div className="stat-card">
                        <span className="stat-label">Total Orders</span>
                        <span className="stat-value">{stats.orderCount}</span>
                    </div>
                    <div className="stat-card">
                        <span className="stat-label">Avg Order Value</span>
                        <span className="stat-value">₹{stats.avgValue.toFixed(2)}</span>
                    </div>
                    <div className="stat-card">
                        <span className="stat-label">Top Restaurant</span>
                        <span className="stat-value" style={{ fontSize: '1.2rem', textTransform: 'capitalize' }}>
                            {stats.topRest.replace(/_/g, ' ')}
                        </span>
                    </div>
                </div>

                {orders.length > 0 ? (
                    <>
                        <div className="chart-container">
                            <h3 className="chart-title">Spending Over Time</h3>
                            <div className="chart-wrapper">
                                <ResponsiveContainer width="100%" height={200}>
                                    <LineChart data={lineData}>
                                        <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.1)" />
                                        <XAxis dataKey="date" stroke="#9ca3af" fontSize={12} tickLine={false} axisLine={false} />
                                        <YAxis stroke="#9ca3af" fontSize={12} tickLine={false} axisLine={false} />
                                        <Tooltip 
                                            contentStyle={{ background: '#111827', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '8px' }}
                                            itemStyle={{ color: '#fff' }}
                                        />
                                        <Line type="monotone" dataKey="amount" stroke="#f97316" strokeWidth={3} dot={{ fill: '#ec4899', r: 4 }} activeDot={{ r: 6 }} />
                                    </LineChart>
                                </ResponsiveContainer>
                            </div>
                        </div>

                        {pieData.length > 0 && (
                            <div className="chart-container">
                                <h3 className="chart-title">Orders by Restaurant</h3>
                                <div className="chart-wrapper" style={{ display: 'flex', justifyContent: 'center' }}>
                                    <PieChart width={200} height={200}>
                                        <Pie
                                            data={pieData}
                                            cx={100}
                                            cy={100}
                                            innerRadius={60}
                                            outerRadius={80}
                                            paddingAngle={5}
                                            dataKey="value"
                                            stroke="none"
                                        >
                                            {pieData.map((entry, index) => (
                                                <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                            ))}
                                        </Pie>
                                        <Tooltip 
                                            contentStyle={{ background: '#111827', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '8px' }}
                                            itemStyle={{ color: '#fff' }}
                                        />
                                    </PieChart>
                                </div>
                            </div>
                        )}
                    </>
                ) : (
                    <div className="analytics-empty">
                        <div className="empty-icon">🍽️</div>
                        <p>No orders yet. Place your first order to see analytics!</p>
                    </div>
                )}
            </div>
        </aside>
    );
}
