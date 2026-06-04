import { useEffect, useState } from "react";
import { getBackendHealth } from "./services/healthService";

function App() {
  const [backendStatus, setBackendStatus] = useState("Checking...");
  const [backendService, setBackendService] = useState("-");
  const [error, setError] = useState("");

  useEffect(() => {
    async function checkBackend() {
      try {
        const data = await getBackendHealth();
        setBackendStatus(data.status);
        setBackendService(data.service);
      } catch (err) {
        setBackendStatus("DOWN");
        setBackendService("-");
        setError("Backend connection failed.");
      }
    }

    checkBackend();
  }, []);

  return (
    <main className="app-container">
      <section className="hero-card">
        <p className="eyebrow">Personal Finance Tracker</p>
        <h1>SubTrack</h1>
        <p>
          Track subscriptions, upcoming payments, budgets, and spending insights
          from one dashboard.
        </p>

        <div className="status-card">
          <strong>Backend Status:</strong>
          <span>{backendStatus}</span>
        </div>

        <div className="status-card">
          <strong>Backend Service:</strong>
          <span>{backendService}</span>
        </div>

        {error && <p className="error-message">{error}</p>}
      </section>
    </main>
  );
}

export default App;