import React, { useState } from "react";
import MoodForm from "./components/MoodForm";
import ResultPage from "./pages/ResultPage";
import Loader from "./components/Loader";
import { getRecommendations } from "./services/api";
import "./App.css";

function App() {
  const [songs, setSongs] = useState([]);
  const [mood, setMood] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [showResults, setShowResults] = useState(false);

  const handleSearch = async (moodText, limit) => {
    setLoading(true);
    setError(null);
    setMood(moodText);
    setSongs([]);

    try {
      const data = await getRecommendations(moodText, limit);
      setSongs(data);
      setShowResults(true);
    } catch (err) {
      setError(err.message || "Something went wrong. Please try again.");
      setShowResults(false);
    } finally {
      setLoading(false);
    }
  };

  const handleBack = () => {
    setShowResults(false);
    setSongs([]);
    setError(null);
  };

  const handleRefine = (newMood) => {
    handleSearch(newMood, 5);
  };

  return (
    <>
      <div className="app-bg" aria-hidden="true">
        <div className="orb orb-1" />
        <div className="orb orb-2" />
        <div className="orb orb-3" />
      </div>

      <div className="app-container">
        <header className="app-header">
          <div className="app-logo" onClick={handleBack} role="button" tabIndex={0} aria-label="Go home">
            <div className="app-logo-icon">🎵</div>
            <span className="app-logo-text">LyricMind</span>
          </div>
          <span className="app-logo-badge">AI Powered</span>
        </header>

        <main className="app-main">
          {loading ? (
            <Loader />
          ) : error ? (
            <div className="error-container">
              <div className="error-icon">⚠️</div>
              <p className="error-message">Couldn't load recommendations</p>
              <p className="error-hint">{error}</p>
              <div className="error-actions">
                <button className="retry-btn" onClick={() => handleSearch(mood, 5)}>
                  Try Again
                </button>
                <button className="back-btn" onClick={handleBack}>
                  New Search
                </button>
              </div>
            </div>
          ) : showResults ? (
            <ResultPage
              songs={songs}
              mood={mood}
              onBack={handleBack}
              onRefine={handleRefine}
            />
          ) : (
            <MoodForm onSearch={handleSearch} />
          )}
        </main>

        <footer className="app-footer">
          <div>Built with RAG architecture</div>
          <div className="footer-tech">Spring Boot · MongoDB Atlas · OpenAI · React</div>
        </footer>
      </div>
    </>
  );
}

export default App;