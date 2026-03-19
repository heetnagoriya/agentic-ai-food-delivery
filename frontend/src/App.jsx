import { useState, useEffect, useCallback, useRef } from 'react';
import AuthPage from './components/Auth/AuthPage';
import Sidebar from './components/Sidebar/Sidebar';
import ChatArea from './components/Chat/ChatArea';
import StatePanel from './components/StatePanel/StatePanel';
import DeliveryMap from './components/Delivery/DeliveryMap';
import ToastContainer, { useToast } from './components/common/Toast';
import { askAgentStream, getWorldState, resetUser, getUserProfile, toggleSurge } from './utils/api';
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

function loadMessages(userId) {
  try {
    const data = sessionStorage.getItem(`cafe_messages_${userId}`);
    return data ? JSON.parse(data) : [];
  } catch { return []; }
}

function saveMessages(userId, messages) {
  try { sessionStorage.setItem(`cafe_messages_${userId}`, JSON.stringify(messages)); }
  catch { /* ignore */ }
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
  const [sidebarCollapsed, setSidebarCollapsed] = useState(window.innerWidth < 768);
  const [showStatePanel, setShowStatePanel] = useState(window.innerWidth >= 1200);
  const { toasts, addToast, removeToast } = useToast();
  const worldStateTimerRef = useRef(null);

  const userId = user?.userId;
  const userName = user?.name;

  // Load messages when user changes
  useEffect(() => {
    if (userId) {
      setMessages(loadMessages(userId));
    }
  }, [userId]);

  // Save messages when they change
  useEffect(() => {
    if (userId && messages.length > 0) {
      saveMessages(userId, messages);
    }
  }, [messages, userId]);

  // Fetch world state
  const fetchWorldState = useCallback(async () => {
    try {
      const data = await getWorldState();
      setWorldState(data);
    } catch (err) {
      console.error('World state fetch failed:', err);
    }
  }, []);

  const prevWorldStateRef = useRef(null);

  // Detect order status changes
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

  // Responsive sidebar & state panel
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

  const toggleTheme = useCallback(() => {
    setTheme(t => t === 'dark' ? 'light' : 'dark');
  }, []);

  // Login
  const handleLogin = useCallback((userData) => {
    setUser(userData);
    setMessages(loadMessages(userData.userId));
  }, []);

  // Logout
  const handleLogout = useCallback(() => {
    localStorage.removeItem('cafe_user');
    setUser(null);
    setMessages([]);
    setWorldState(null);
  }, []);

  // Send message (using SSE streaming for real-time trace updates)
  const handleSend = useCallback((text) => {
    const userMsg = {
      id: ++messageIdCounter,
      role: 'user',
      text,
      time: getTimeNow(),
    };
    setMessages((prev) => [...prev, userMsg]);
    setIsLoading(true);

    // Create a placeholder AI message that we'll update with streaming data
    const aiMsgId = ++messageIdCounter;
    const aiMsg = {
      id: aiMsgId,
      role: 'ai',
      text: '',
      time: getTimeNow(),
      confidence: 0,
      trace: [],
      isStreaming: true,
    };
    setMessages((prev) => [...prev, aiMsg]);

    const abort = askAgentStream(
      userId,
      text,
      // onTrace — called for each tool call step in real-time
      (traceStep) => {
        setMessages((prev) =>
          prev.map((msg) =>
            msg.id === aiMsgId
              ? { ...msg, trace: [...msg.trace, traceStep] }
              : msg
          )
        );
      },
      // onResult — called when the final response is ready
      (data) => {
        setMessages((prev) =>
          prev.map((msg) =>
            msg.id === aiMsgId
              ? {
                  ...msg,
                  text: data.message || 'No response from agent.',
                  confidence: data.confidence,
                  trace: data.trace || msg.trace,
                  isStreaming: false,
                }
              : msg
          )
        );
        setIsLoading(false);
        fetchWorldState();
      },
      // onError — called on failure
      (errorMsg) => {
        setMessages((prev) =>
          prev.map((msg) =>
            msg.id === aiMsgId
              ? {
                  ...msg,
                  text: '⚠️ ' + (errorMsg || 'Sorry, I encountered an error. Please try again.'),
                  isStreaming: false,
                }
              : msg
          )
        );
        setIsLoading(false);
        addToast(errorMsg || 'Failed to get a response.', 'error');
      }
    );

    // Store abort function for potential cleanup
    return () => abort();
  }, [userId, addToast, fetchWorldState]);

  // New chat
  const handleNewChat = useCallback(() => {
    setMessages([]);
    if (userId) {
      saveMessages(userId, []);
    }
  }, [userId]);

  // Reset memory
  const handleResetMemory = useCallback(async () => {
    try {
      await resetUser(userId);
      addToast('Agent memory reset successfully!', 'success');
      setMessages([]);
      if (userId) saveMessages(userId, []);
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

  // Find delivery orders (OUT_FOR_DELIVERY)
  const activeOrders = worldState?.active_orders || {};
  const deliveryOrder = Object.values(activeOrders).find(
    (o) => o.status === 'OUT_FOR_DELIVERY' && !o.isCancelled
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
      />

      <main className="main-content" id="main-content">
        <div className="main-chat">
          <ChatArea
            messages={messages}
            isLoading={isLoading}
            onSend={handleSend}
          />
        </div>

        {showStatePanel && (
          <StatePanel
            worldState={worldState}
            userId={userId}
            onToggleSurge={handleToggleSurge}
          />
        )}
      </main>

      {deliveryOrder && <DeliveryMap order={deliveryOrder} />}

      {/* Mobile toggle for state panel */}
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
