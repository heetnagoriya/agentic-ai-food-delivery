const BASE_URL = 'http://localhost:8080';

/**
 * Original synchronous API call — kept as fallback.
 */
export async function askAgent(userId, question) {
  const params = new URLSearchParams({ userId, question });
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 120000);

  try {
    const res = await fetch(`${BASE_URL}/ask?${params}`, {
      signal: controller.signal,
    });
    clearTimeout(timeoutId);
    if (!res.ok) throw new Error(`Server error: ${res.status}`);
    return await res.json();
  } catch (err) {
    clearTimeout(timeoutId);
    if (err.name === 'AbortError') {
      throw new Error('Request timed out after 120 seconds. The AI agent may be processing a complex request.');
    }
    throw err;
  }
}

/**
 * SSE streaming API — receives trace steps in real-time.
 * @param {string} userId
 * @param {string} question
 * @param {Function} onTrace  - called with each TraceStep object as it arrives
 * @param {Function} onResult - called with the final AgentResponse
 * @param {Function} onError  - called with an error message string
 * @returns {Function} abort function to cancel the stream
 */
export function askAgentStream(userId, question, onTrace, onResult, onError) {
  const params = new URLSearchParams({ userId, question });
  const controller = new AbortController();

  // Use fetch + ReadableStream because EventSource only supports GET without custom headers
  fetch(`${BASE_URL}/ask/stream?${params}`, { signal: controller.signal })
    .then((res) => {
      if (!res.ok) {
        throw new Error(`Server error: ${res.status}`);
      }

      const reader = res.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';

      function processChunk() {
        reader.read().then(({ done, value }) => {
          if (done) return;

          buffer += decoder.decode(value, { stream: true });

          // SSE format: "event: <name>\ndata: <json>\n\n"
          const events = buffer.split('\n\n');
          // Keep the last incomplete chunk in the buffer
          buffer = events.pop() || '';

          for (const event of events) {
            if (!event.trim()) continue;

            const lines = event.split('\n');
            let eventName = '';
            let eventData = '';

            for (const line of lines) {
              if (line.startsWith('event:')) {
                eventName = line.slice(6).trim();
              } else if (line.startsWith('data:')) {
                eventData = line.slice(5).trim();
              }
            }

            if (!eventData) continue;

            try {
              const parsed = JSON.parse(eventData);

              if (eventName === 'trace') {
                onTrace(parsed);
              } else if (eventName === 'result') {
                onResult(parsed);
              } else if (eventName === 'error') {
                onError(typeof parsed === 'string' ? parsed : JSON.stringify(parsed));
              }
            } catch (parseErr) {
              // If data isn't valid JSON, treat it as a string (error messages)
              if (eventName === 'error') {
                onError(eventData);
              }
            }
          }

          processChunk();
        }).catch((err) => {
          if (err.name !== 'AbortError') {
            onError(err.message || 'Stream read error');
          }
        });
      }

      processChunk();
    })
    .catch((err) => {
      if (err.name !== 'AbortError') {
        onError(err.message || 'Failed to connect to streaming endpoint');
      }
    });

  // Return abort function
  return () => controller.abort();
}

export async function getWorldState() {
  const res = await fetch(`${BASE_URL}/fake-swiggy/world-state`);
  if (!res.ok) throw new Error(`Failed to fetch world state: ${res.status}`);
  return await res.json();
}

export async function resetUser(userId) {
  const params = new URLSearchParams({ userId });
  const res = await fetch(`${BASE_URL}/user/reset?${params}`, {
    method: 'DELETE',
  });
  if (!res.ok) throw new Error(`Failed to reset user: ${res.status}`);
  return true;
}

export async function getUserProfile(userId) {
  const params = new URLSearchParams({ userId });
  const res = await fetch(`${BASE_URL}/user/profile?${params}`);
  if (!res.ok) throw new Error(`Failed to fetch user profile: ${res.status}`);
  return await res.json();
}

export async function toggleSurge(enable) {
  const endpoint = enable ? 'on' : 'off';
  const res = await fetch(`${BASE_URL}/fake-swiggy/surge/${endpoint}`, {
    method: 'POST',
  });
  if (!res.ok) throw new Error(`Failed to toggle surge: ${res.status}`);
  return await res.text();
}

export async function trackOrderAPI(orderId) {
  const res = await fetch(`${BASE_URL}/fake-swiggy/track/${orderId}`);
  if (!res.ok) throw new Error(`Failed to track order: ${res.status}`);
  return await res.json();
}

export async function searchMenu(query) {
  const params = new URLSearchParams({ query });
  const res = await fetch(`${BASE_URL}/fake-swiggy/search?${params}`);
  if (!res.ok) throw new Error(`Failed to search menu: ${res.status}`);
  return await res.json();
}
