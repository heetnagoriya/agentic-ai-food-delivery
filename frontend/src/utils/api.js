const BASE_URL = 'http://localhost:8080';

export function getAuthToken() {
  return localStorage.getItem('jwt_token');
}

export function setAuthToken(token, userId) {
  localStorage.setItem('jwt_token', token);
  localStorage.setItem('user_id', userId);
}

export function logout() {
  localStorage.removeItem('jwt_token');
  localStorage.removeItem('user_id');
}

export function getAuthHeaders() {
  const token = getAuthToken();
  return token ? { 'Authorization': `Bearer ${token}` } : {};
}

export async function loginWithApi(email, password) {
  const res = await fetch(`${BASE_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  });
  if (!res.ok) throw new Error(`Login failed: ${res.statusText}`);
  const data = await res.json();
  setAuthToken(data.jwt, data.userId);
  return data;
}

export async function googleLoginApi(credential) {
  const res = await fetch(`${BASE_URL}/auth/google`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ credential })
  });
  if (!res.ok) {
    const errText = await res.text();
    throw new Error(`Google Login failed: ${errText}`);
  }
  const data = await res.json();
  setAuthToken(data.jwt, data.userId);
  return data;
}

export async function sendOtpApi(email) {
  const res = await fetch(`${BASE_URL}/auth/send-otp`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email })
  });
  if (!res.ok) {
    const errText = await res.text();
    throw new Error(`Failed to send OTP: ${errText}`);
  }
  return await res.json();
}

export async function registerWithApi(name, email, password, otp) {
  const res = await fetch(`${BASE_URL}/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, email, password, otp })
  });
  if (!res.ok) {
    const errText = await res.text();
    throw new Error(`Register failed: ${errText}`);
  }
  const data = await res.json();
  setAuthToken(data.jwt, data.userId);
  return data;
}

/** Synchronous agent ask with 120s timeout */
export async function askAgent(userId, question) {
  const params = new URLSearchParams({ userId, question });
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 120000);

  try {
    const res = await fetch(`${BASE_URL}/ask?${params}`, {
      headers: getAuthHeaders(),
      signal: controller.signal,
    });
    clearTimeout(timeoutId);
    if (!res.ok) throw new Error(`Server error: ${res.status}`);
    return await res.json();
  } catch (err) {
    clearTimeout(timeoutId);
    if (err.name === 'AbortError') {
      throw new Error('Request timed out. The AI is processing a complex request.');
    }
    throw err;
  }
}

/** SSE streaming agent ask — used on localhost */
export function askAgentStream(userId, question, onTrace, onResult, onError) {
  const params = new URLSearchParams({ userId, question });
  const controller = new AbortController();

  fetch(`${BASE_URL}/ask/stream?${params}`, { signal: controller.signal, headers: getAuthHeaders() })
    .then((res) => {
      if (!res.ok) throw new Error(`Server error: ${res.status}`);

      const reader = res.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';

      function processChunk() {
        reader.read().then(({ done, value }) => {
          if (done) return;
          buffer += decoder.decode(value, { stream: true });
          const events = buffer.split('\n\n');
          buffer = events.pop() || '';

          for (const event of events) {
            if (!event.trim()) continue;
            const lines = event.split('\n');
            let eventName = '', eventData = '';
            for (const line of lines) {
              if (line.startsWith('event:')) eventName = line.slice(6).trim();
              else if (line.startsWith('data:')) eventData = line.slice(5).trim();
            }
            if (!eventData) continue;
            try {
              const parsed = JSON.parse(eventData);
              if (eventName === 'trace') onTrace(parsed);
              else if (eventName === 'result') onResult(parsed);
              else if (eventName === 'error') onError(typeof parsed === 'string' ? parsed : JSON.stringify(parsed));
            } catch {
              if (eventName === 'error') onError(eventData);
            }
          }
          processChunk();
        }).catch((err) => { if (err.name !== 'AbortError') onError(err.message); });
      }
      processChunk();
    })
    .catch((err) => { if (err.name !== 'AbortError') onError(err.message || 'Failed to connect'); });

  return () => controller.abort();
}

export async function getWorldState(userId) {
  const params = userId ? `?userId=${encodeURIComponent(userId)}` : '';
  const res = await fetch(`${BASE_URL}/fake-swiggy/world-state${params}`, { headers: getAuthHeaders() });
  if (!res.ok) throw new Error(`Failed to fetch world state: ${res.status}`);
  return await res.json();
}

export async function resetUser(userId) {
  const params = new URLSearchParams({ userId });
  const res = await fetch(`${BASE_URL}/user/reset?${params}`, { headers: getAuthHeaders(), method: 'DELETE' });
  if (!res.ok) throw new Error(`Failed to reset user: ${res.status}`);
  return true;
}

export async function getUserProfile(userId) {
  const params = new URLSearchParams({ userId });
  const res = await fetch(`${BASE_URL}/user/profile?${params}`, { headers: getAuthHeaders() });
  if (!res.ok) throw new Error(`Failed to fetch user profile: ${res.status}`);
  return await res.json();
}

export async function updateUserProfile(userId, profileData) {
  const params = new URLSearchParams({ userId });
  const res = await fetch(`${BASE_URL}/user/profile?${params}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
    body: JSON.stringify(profileData)
  });
  if (!res.ok) throw new Error(`Failed to update profile: ${res.status}`);
  return await res.json();
}

export async function toggleSurge(enable) {
  const endpoint = enable ? 'on' : 'off';
  const res = await fetch(`${BASE_URL}/fake-swiggy/surge/${endpoint}`, { method: 'POST', headers: getAuthHeaders() });
  if (!res.ok) throw new Error(`Failed to toggle surge: ${res.status}`);
  return await res.text();
}

export async function trackOrderAPI(orderId) {
  const res = await fetch(`${BASE_URL}/fake-swiggy/track/${orderId}`, { headers: getAuthHeaders() });
  if (!res.ok) throw new Error(`Failed to track order: ${res.status}`);
  return await res.json();
}

export async function searchMenu(query) {
  const params = new URLSearchParams({ query });
  const res = await fetch(`${BASE_URL}/fake-swiggy/search?${params}`, { headers: getAuthHeaders() });
  if (!res.ok) throw new Error(`Failed to search menu: ${res.status}`);
  return await res.json();
}

export async function getOrderHistory(userId) {
  const params = new URLSearchParams({ userId });
  const res = await fetch(`${BASE_URL}/fake-swiggy/history?${params}`, { headers: getAuthHeaders() });
  if (!res.ok) throw new Error(`Failed to fetch order history: ${res.status}`);
  return await res.json();
}

// ============ NEW: Chat History ============

export async function getChatHistory(userId) {
  const params = new URLSearchParams({ userId });
  const res = await fetch(`${BASE_URL}/api/chat/history?${params}`, { headers: getAuthHeaders() });
  if (!res.ok) return [];
  return await res.json();
}

export async function clearChatHistory(userId) {
  const params = new URLSearchParams({ userId });
  const res = await fetch(`${BASE_URL}/api/chat/history?${params}`, { method: 'DELETE', headers: getAuthHeaders() });
  if (!res.ok) throw new Error('Failed to clear chat history');
  return await res.json();
}

// ============ NEW: Onboarding ============

export async function getOnboardingStatus(userId) {
  const params = new URLSearchParams({ userId });
  const res = await fetch(`${BASE_URL}/api/onboarding/status?${params}`, { headers: getAuthHeaders() });
  if (!res.ok) return { onboardingComplete: false };
  return await res.json();
}

export async function completeOnboarding(data) {
  const res = await fetch(`${BASE_URL}/api/onboarding/complete`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
    body: JSON.stringify(data)
  });
  if (!res.ok) throw new Error('Failed to save onboarding preferences');
  return await res.json();
}

// ============ NEW: Stripe Payments ============

export async function createStripeCheckout(userId, amount) {
  const res = await fetch(`${BASE_URL}/api/payments/create-checkout`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
    body: JSON.stringify({ userId, amount })
  });
  if (!res.ok) throw new Error('Failed to create payment session');
  return await res.json();
}

export async function verifyStripePayment(sessionId, userId, amount) {
  const res = await fetch(`${BASE_URL}/api/payments/verify`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
    body: JSON.stringify({ sessionId, userId, amount: Number(amount) })
  });
  if (!res.ok) throw new Error('Failed to verify payment');
  return await res.json();
}
