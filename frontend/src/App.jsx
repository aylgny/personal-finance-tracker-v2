import { useEffect, useState } from "react";
import { getBackendHealth } from "./services/healthService";
import { getSubscriptions } from "./services/subscriptionService";

function App() {
  const [backendStatus, setBackendStatus] = useState("Checking...");
  const [backendService, setBackendService] = useState("-");
  const [subscriptions, setSubscriptions] = useState([]);
  const [error, setError] = useState("");

  useEffect(() => {
    async function loadInitialData() {
      try {
        const healthData = await getBackendHealth();
        setBackendStatus(healthData.status);
        setBackendService(healthData.service);

        const subscriptionData = await getSubscriptions();
        setSubscriptions(subscriptionData);
      } catch (err) {
        setBackendStatus("DOWN");
        setBackendService("-");
        setError("Backend connection failed.");
      }
    }

    loadInitialData();
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

        <section className="subscription-section">
          <h2>Subscriptions</h2>

          {subscriptions.length === 0 && !error ? (
            <p>No subscriptions found.</p>
          ) : (
            <div className="subscription-grid">
              {subscriptions.map((subscription) => (
                <article className="subscription-card" key={subscription.id}>
                  <div>
                    <h3>{subscription.name}</h3>
                    <p>{subscription.categoryName}</p>
                  </div>

                  <div className="subscription-price">
                    {subscription.currencySymbol}
                    {subscription.price}
                  </div>

                  <div className="subscription-meta">
                    <span>{subscription.billingCycle}</span>
                    <span>Next: {subscription.nextPaymentDate}</span>
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>
      </section>
    </main>
  );
}

export default App;