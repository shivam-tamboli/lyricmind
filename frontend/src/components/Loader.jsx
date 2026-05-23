import React, { useState, useEffect } from "react";

const STEPS = [
  "Analyzing your mood...",
  "Searching music library...",
  "Ranking with AI...",
  "Personalizing results...",
];

const Loader = () => {
  const [step, setStep] = useState(0);

  useEffect(() => {
    const timers = [
      setTimeout(() => setStep(1), 1200),
      setTimeout(() => setStep(2), 2800),
      setTimeout(() => setStep(3), 4800),
    ];
    return () => timers.forEach(clearTimeout);
  }, []);

  return (
    <div className="loader-overlay">
      <div className="equalizer" aria-hidden="true">
        {[...Array(5)].map((_, i) => (
          <div key={i} className="eq-bar" />
        ))}
      </div>
      <p className="loader-text">{STEPS[step]}</p>
      <div className="loader-steps" role="progressbar" aria-valuenow={step + 1} aria-valuemax={STEPS.length}>
        {STEPS.map((_, i) => (
          <div key={i} className={`step-dot ${i <= step ? "active" : ""}`} />
        ))}
      </div>
      <p className="loader-subtext">Semantic search + AI reranking</p>
    </div>
  );
};

export default Loader;