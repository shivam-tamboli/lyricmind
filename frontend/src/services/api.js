const API_BASE = import.meta.env.VITE_API_URL || "http://localhost:8080";

const TIMEOUT_MS = 45_000;

export const getRecommendations = async (mood, limit, signal) => {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), TIMEOUT_MS);

  // Allow the caller to cancel from outside (e.g. component unmount)
  if (signal) {
    signal.addEventListener("abort", () => controller.abort());
  }

  try {
    const response = await fetch(`${API_BASE}/api/lyricmind/v1/recommendations`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ mood, limit }),
      signal: controller.signal,
    });

    if (!response.ok) {
      const errorText = await response.text().catch(() => "Unknown error");
      throw new Error(errorText || `Request failed with status ${response.status}`);
    }

    return await response.json();
  } catch (err) {
    if (err.name === "AbortError") {
      throw new Error(
        "Request timed out. The server may be starting up — please try again in a moment."
      );
    }
    throw err;
  } finally {
    clearTimeout(timeoutId);
  }
};

export const pingServer = () =>
  fetch(`${API_BASE}/actuator/health`, { method: "GET" }).catch(() => {});
