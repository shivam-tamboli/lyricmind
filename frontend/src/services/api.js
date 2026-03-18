const API_BASE = import.meta.env.VITE_API_URL || "http://localhost:8080";

export const getRecommendations = async (mood, limit) => {
  const response = await fetch(`${API_BASE}/api/lyricmind/v1/recommendations`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ mood, limit }),
  });

  if (!response.ok) {
    const errorText = await response.text().catch(() => "Unknown error");
    throw new Error(errorText || `Request failed with status ${response.status}`);
  }

  return await response.json();
};