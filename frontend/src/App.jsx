import { useEffect, useState } from "react";
import { getBackendHealth } from "./services/healthService";
import { getSubscriptions } from "./services/subscriptionService";
import { login } from "./services/authService";

function App() {
  const [backendStatus, setBackendStatus] = useState("Checking...");
  const [backendService, setBackendService] = useState("-");
  const [subscriptions, setSubscriptions] = useState([]);
  const [error, setError] = useState("");
  const [authUser, setAuthUser] = useState(() => {
    const savedUser = localStorage.getItem("subtrack_user");
    return savedUser ? JSON.parse(savedUser) : null;
  });

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  async function loadSubscriptions() {
    const subscriptionData = await getSubscriptions();
    setSubscriptions(subscriptionData);
  }

  useEffect(() => {
    async function loadHealth() {
      try {
        const healthData = await getBackendHealth();
        setBackendStatus(healthData.status);
        setBackendService(healthData.service);
      } catch (err) {
        setBackendStatus("DOWN");
        setBackendService("-");
        setError("Backend connection failed.");
      }
    }

    loadHealth();
  }, []);

  useEffect(() => {
    if (!authUser) {
      setSubscriptions([]);
      return;
    }

    loadSubscriptions().catch(() => {
      setError("Could not load subscriptions.");
    });
  }, [authUser]);

  async function handleLogin(event) {
    event.preventDefault();
    setError("");

    try {
      const data = await login(email, password);

      localStorage.setItem("subtrack_token", data.token);
      localStorage.setItem(
        "subtrack_user",
        JSON.stringify({
          userId: data.userId,
          name: data.name,
          email: data.email,
        })
      );

      setAuthUser({
        userId: data.userId,
        name: data.name,
        email: data.email,
      });
    } catch (err) {
      setError("Login failed. Please check your email and password.");
    }
  }

  function handleLogout() {
    localStorage.removeItem("subtrack_token");
    localStorage.removeItem("subtrack_user");
    setAuthUser(null);
    setSubscriptions([]);
  }

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

        {!authUser ? (
          <form className="login-form" onSubmit={handleLogin}>
          <h2>Login</h2>
        
          <div className="form-field">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              value={email}
              placeholder="Enter your email"
              onChange={(event) => setEmail(event.target.value)}
            />
          </div>
        
          <div className="form-field">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              value={password}
              placeholder="Enter your password"
              onChange={(event) => setPassword(event.target.value)}
            />
          </div>
        
          <button type="submit">Login</button>
        </form>
        ) : (
          <>
            <div className="user-row">
              <div>
                <strong>Logged in as:</strong> {authUser.name} ({authUser.email})
              </div>
              <button type="button" onClick={handleLogout}>
                Logout
              </button>
            </div>

            <section className="subscription-section">
              <h2>Subscriptions</h2>

              {subscriptions.length === 0 ? (
                <p>No subscriptions found for this user.</p>
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
          </>
        )}

        {error && <p className="error-message">{error}</p>}
      </section>
    </main>
  );
}

export default App;