import React, { useState, useEffect } from "react";

const STEPS = [
  { text: "Analyzing your mood...",      delay: 0 },
  { text: "Searching music library...",  delay: 1200 },
  { text: "Ranking with AI...",          delay: 2800 },
  { text: "Personalizing results...",    delay: 4800 },
  // Cold-start messages — shown only when the pipeline takes > ~10s
  { text: "Server is starting up...",    delay: 10000 },
  { text: "Almost there, hang tight...", delay: 20000 },
];

const Loader = () => {
  const [step, setStep] = useState(0);

  useEffect(() => {
    const timers = STEPS.slice(1).map(({ delay }, i) =>
      setTimeout(() => setStep(i + 1), delay)
    );
    return () => timers.forEach(clearTimeout);
  }, []);

  const isSlowStart = step >= 4;

  return (
    <div className="loader-overlay">
      <div className="equalizer" aria-hidden="true">
        {[...Array(5)].map((_, i) => (
          <div key={i} className="eq-bar" />
        ))}
      </div>
      <p className="loader-text">{STEPS[step].text}</p>
      <div
        className="loader-steps"
        role="progressbar"
        aria-valuenow={Math.min(step + 1, 4)}
        aria-valuemax={4}
      >
        {STEPS.slice(0, 4).map((_, i) => (
          <div key={i} className={`step-dot ${i <= Math.min(step, 3) ? "active" : ""}`} />
        ))}
      </div>
      <p className="loader-subtext">
        {isSlowStart
          ? "Free-tier server waking from sleep — first request takes ~30s"
          : "Semantic search + AI reranking"}
      </p>
    </div>
  );
};

export default Loader;
