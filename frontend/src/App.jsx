import { useState, useEffect, useCallback, useRef } from 'react';
import AuthPage from './components/Auth/AuthPage';
import OnboardingPage from './components/Auth/OnboardingPage';
import Sidebar from './components/Sidebar/Sidebar';
import ChatArea from './components/Chat/ChatArea';
import StatePanel from './components/StatePanel/StatePanel';
import AnalyticsPanel from './components/StatePanel/AnalyticsPanel';
import DeliveryMap from './components/Delivery/DeliveryMap';
import ToastContainer, { useToast } from './components/common/Toast';
import {
  askAgentStream, getWorldState, resetUser, toggleSurge,
  getChatHistory, clearChatHistory, getOnboardingStatus,
  verifyStripePayment
} from './utils/api';
import './App.css';

let messageIdCounter = 0;

function getTimeNow() {
  return new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
}

function playNotificationSound() {
  try {
    const AudioContext = window.AudioContext || window.webkitAudioContext;
    if (!AudioContext) return;
    const ctx = new AudioContext();
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.connect(gain);
    gain.connect(ctx.destination);
    osc.type = 'sine';
    osc.frequency.setValueAtTime(880, ctx.currentTime);
    gain.gain.setValueAtTime(0.1, ctx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.1);
    osc.start();
    osc.stop(ctx.currentTime + 0.1);
  } catch(e) {}
}

function getSavedUser() {
  try {
    const data = localStorage.getItem('cafe_user');
    return data ? JSON.parse(data) : null;
  } catch { return null; }
}

export default function App() {
  const [user, setUser] = useState(() => getSavedUser());
  const [theme, setTheme] = useState(() => localStorage.getItem('cafe_theme') || 'dark');
  const [messages, setMessages] = useState([]);
  const [worldState, setWorldState] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [showOnboarding, setShowOnboarding] = useState(false);
  const [historyLoaded, setHistoryLoaded] = useState(false);
  const [sidebarCollapsed, setSidebarCollapsed] = useState(window.innerWidth < 768);
  const [showStatePanel, setShowStatePanel] = useState(window.innerWidth >= 1200);
  const [showAnalytics, setShowAnalytics] = useState(false);
  const [autonomyLevel, setAutonomyLevel] = useState(() => localStorage.getItem('cafe_autonomy') || 'balanced');
  const { toasts, addToast, removeToast } = useToast();
  const worldStateTimerRef = useRef(null);

  const userId = user?.userId;
  const userName = user?.name;

  // Persist autonomy level
  useEffect(() => {
    localStorage.setItem('cafe_autonomy', autonomyLevel);
  }, [autonomyLevel]);

  // Load DynamoDB chat history when user logs in
  useEffect(() => {
    if (!userId || historyLoaded) return;
    getChatHistory(userId).then((history) => {
      if (history && history.length > 0) {
        const mapped = history.map((m) => ({
          id: ++messageIdCounter,
          role: m.role,
          text: m.text,
          time: m.time || getTimeNow(),
          confidence: m.confidence || 0,
          trace: m.trace || [],
          isStreaming: false,
        }));
        setMessages(mapped);
      }
      setHistoryLoaded(true);
    }).catch(() => setHistoryLoaded(true));
  }, [userId, historyLoaded]);

  // Reset history loaded flag on logout
  useEffect(() => {
    if (!userId) setHistoryLoaded(false);
  }, [userId]);

  // Check if onboarding is needed after login
  useEffect(() => {
    if (!userId) return;
    getOnboardingStatus(userId).then((status) => {
      if (!status.onboardingComplete) setShowOnboarding(true);
    }).catch(() => {});
  }, [userId]);

  // Fetch world state
  const fetchWorldState = useCallback(async () => {
    if (!userId) return;
    try {
      const data = await getWorldState(userId);
      setWorldState(data);
    } catch (err) {
      console.error('World state fetch failed:', err);
    }
  }, [userId]);

  const processedSessionRef = useRef(null);

  // Handle Stripe Payment Success Redirect
  useEffect(() => {
    if (!userId) return;
    const path = window.location.pathname;
    const params = new URLSearchParams(window.location.search);
    const sessionId = params.get('session_id');
    
    if (path === '/payment-success' && sessionId) {
      if (processedSessionRef.current === sessionId) return;
      processedSessionRef.current = sessionId;

      const amount = params.get('amount') || 500;
      verifyStripePayment(sessionId, userId, amount)
        .then(res => {
          if (res.status === 'success') {
            addToast(`Successfully added ₹${amount} to your wallet!`, 'success');
            fetchWorldState();
          } else {
            addToast(res.message || 'Payment verification failed.', 'error');
          }
        })
        .catch(err => addToast('Verification error: ' + err.message, 'error'))
        .finally(() => {
          window.history.replaceState({}, document.title, '/');
        });
    }
  }, [userId, addToast, fetchWorldState]);

  const prevWorldStateRef = useRef(null);

  // Detect order status changes and notify
  useEffect(() => {
    if (prevWorldStateRef.current?.active_orders && worldState?.active_orders) {
      const prevOrders = prevWorldStateRef.current.active_orders;
      const newOrders = worldState.active_orders;
      Object.entries(newOrders).forEach(([id, order]) => {
        const prevOrder = prevOrders[id];
        if (prevOrder && prevOrder.status !== order.status) {
          playNotificationSound();
          addToast(
            `Order ${id}: ${prevOrder.status.replace(/_/g, ' ')} → ${order.status.replace(/_/g, ' ')}`,
            'info'
          );
        }
      });
    }
    prevWorldStateRef.current = worldState;
  }, [worldState, addToast]);

  // Auto-refresh world state every 10s
  useEffect(() => {
    if (!userId) return;
    fetchWorldState();
    worldStateTimerRef.current = setInterval(fetchWorldState, 10000);
    return () => clearInterval(worldStateTimerRef.current);
  }, [userId, fetchWorldState]);

  // Responsive layout
  useEffect(() => {
    const handleResize = () => {
      if (window.innerWidth >= 768) setSidebarCollapsed(false);
      setShowStatePanel(window.innerWidth >= 1200);
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  // Theme effect
  useEffect(() => {
    document.body.setAttribute('data-theme', theme);
    localStorage.setItem('cafe_theme', theme);
  }, [theme]);

  const toggleTheme = useCallback(() => setTheme(t => t === 'dark' ? 'light' : 'dark'), []);

  // Login
  const handleLogin = useCallback((userData) => {
    setUser(userData);
    setHistoryLoaded(false);
  }, []);

  // Onboarding complete
  const handleOnboardingComplete = useCallback(() => {
    setShowOnboarding(false);
  }, []);

  // Logout
  const handleLogout = useCallback(() => {
    localStorage.removeItem('cafe_user');
    setUser(null);
    setMessages([]);
    setWorldState(null);
    setShowOnboarding(false);
  }, []);

  // Send message using SSE streaming
  const handleSend = useCallback((text) => {
    const userMsg = { id: ++messageIdCounter, role: 'user', text, time: getTimeNow() };
    setMessages((prev) => [...prev, userMsg]);
    setIsLoading(true);

    const aiMsgId = ++messageIdCounter;
    const aiMsg = {
      id: aiMsgId, role: 'ai', text: '', time: getTimeNow(),
      confidence: 0, trace: [], isStreaming: true,
    };
    setMessages((prev) => [...prev, aiMsg]);

    const abort = askAgentStream(
      userId, text,
      // onTrace
      (traceStep) => {
        setMessages((prev) =>
          prev.map((msg) => msg.id === aiMsgId
            ? { ...msg, trace: [...msg.trace, traceStep] }
            : msg)
        );
      },
      // onResult
      (data) => {
        setMessages((prev) =>
          prev.map((msg) => msg.id === aiMsgId
            ? { ...msg, text: data.message || 'No response.', confidence: data.confidence, trace: data.trace || msg.trace, isStreaming: false }
            : msg)
        );
        setIsLoading(false);
        fetchWorldState();
      },
      // onError
      (errorMsg) => {
        setMessages((prev) =>
          prev.map((msg) => msg.id === aiMsgId
            ? { ...msg, text: '⚠️ ' + (errorMsg || 'Error. Please try again.'), isStreaming: false }
            : msg)
        );
        setIsLoading(false);
        addToast(errorMsg || 'Failed to get a response.', 'error');
      }
    );
    return () => abort();
  }, [userId, addToast, fetchWorldState]);

  // New chat
  const handleNewChat = useCallback(async () => {
    setMessages([]);
    if (userId) {
      try { await clearChatHistory(userId); } catch { /* ignore */ }
    }
  }, [userId]);

  // Reset agent memory
  const handleResetMemory = useCallback(async () => {
    try {
      await resetUser(userId);
      addToast('Agent memory reset successfully!', 'success');
      setMessages([]);
      if (userId) await clearChatHistory(userId);
    } catch (err) {
      addToast(err.message || 'Failed to reset memory.', 'error');
    }
  }, [userId, addToast]);

  // Toggle surge pricing
  const handleToggleSurge = useCallback(async () => {
    try {
      const currentSurge = worldState?.surge_active || false;
      const result = await toggleSurge(!currentSurge);
      addToast(result, 'info');
      fetchWorldState();
    } catch (err) {
      addToast(err.message || 'Failed to toggle surge.', 'error');
    }
  }, [worldState, addToast, fetchWorldState]);

  // Auth check
  if (!user) {
    return (
      <>
        <AuthPage onLogin={handleLogin} />
        <ToastContainer toasts={toasts} removeToast={removeToast} />
      </>
    );
  }

  // Onboarding check
  if (showOnboarding) {
    return (
      <>
        <OnboardingPage userId={userId} onComplete={handleOnboardingComplete} />
        <ToastContainer toasts={toasts} removeToast={removeToast} />
      </>
    );
  }

  const activeOrders = worldState?.active_orders || {};
  const deliveryOrder = Object.values(activeOrders).find(
    (o) => ['PLACED', 'PREPARING', 'OUT_FOR_DELIVERY'].includes(o.status) && !o.isCancelled
  );

  return (
    <div className="app" id="app">
      <Sidebar
        userName={userName}
        userId={userId}
        onNewChat={handleNewChat}
        onResetMemory={handleResetMemory}
        onLogout={handleLogout}
        orders={activeOrders}
        collapsed={sidebarCollapsed}
        onToggleCollapse={() => setSidebarCollapsed(!sidebarCollapsed)}
        onSend={handleSend}
        theme={theme}
        onToggleTheme={toggleTheme}
        onToggleAnalytics={() => {
          setShowAnalytics(true);
          if (window.innerWidth < 768) setSidebarCollapsed(true);
        }}
        onToggleWorldState={() => {
          setShowAnalytics(false);
          setShowStatePanel(true);
          if (window.innerWidth < 768) setSidebarCollapsed(true);
        }}
        worldState={worldState}
        autonomyLevel={autonomyLevel}
        onAutonomyChange={setAutonomyLevel}
      />

      <main className="main-content" id="main-content">
        <div className="main-chat">
          <ChatArea messages={messages} isLoading={isLoading} onSend={handleSend} />
        </div>

        {showStatePanel && (
          showAnalytics ? (
            <AnalyticsPanel userId={userId} />
          ) : (
            <StatePanel worldState={worldState} userId={userId} onToggleSurge={handleToggleSurge} />
          )
        )}
      </main>

      {deliveryOrder && <DeliveryMap order={deliveryOrder} />}

      {!showStatePanel && (
        <button
          className="state-panel-mobile-toggle"
          id="state-panel-mobile-toggle"
          onClick={() => setShowStatePanel(!showStatePanel)}
          aria-label="Toggle state panel"
        >
          🌐
        </button>
      )}

      <ToastContainer toasts={toasts} removeToast={removeToast} />
    </div>
  );
}
