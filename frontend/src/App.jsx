/* eslint-disable react-hooks/set-state-in-effect */

import { useEffect, useState } from "react";
import { getBackendHealth } from "./services/healthService";
import {
  createSubscription,
  deleteSubscription,
  getSubscriptions,
  updateSubscription,
} from "./services/subscriptionService";
import { login } from "./services/authService";
import {
  getCategories,
  getCurrencies,
  getPaymentMethods,
} from "./services/referenceDataService";

const emptySubscriptionForm = {
  name: "",
  paidBy: "",
  price: "",
  billingCycle: "MONTHLY",
  startDate: "",
  currencyId: "",
  categoryId: "",
  paymentMethodId: "",
  websiteUrl: "",
  notes: "",
};

function formatDate(dateValue) {
  if (!dateValue) {
    return "-";
  }

  return new Date(dateValue).toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
  });
}

function calculateNextPaymentDate(startDate, billingCycle) {
  // Calculates the next payment date from the selected start date and billing cycle.
  if (!startDate) {
    return "";
  }

  const date = new Date(startDate);

  if (billingCycle === "WEEKLY") {
    date.setDate(date.getDate() + 7);
  }

  if (billingCycle === "MONTHLY") {
    date.setMonth(date.getMonth() + 1);
  }

  if (billingCycle === "YEARLY") {
    date.setFullYear(date.getFullYear() + 1);
  }

  return date.toISOString().split("T")[0];
}

function App() {
  const [backendStatus, setBackendStatus] = useState("Checking...");
  const [subscriptions, setSubscriptions] = useState([]);
  const [currencies, setCurrencies] = useState([]);
  const [categories, setCategories] = useState([]);
  const [paymentMethods, setPaymentMethods] = useState([]);
  const [error, setError] = useState("");
  const [searchText, setSearchText] = useState("");
  const [isSubscriptionModalOpen, setIsSubscriptionModalOpen] = useState(false);
  const [editingSubscription, setEditingSubscription] = useState(null);
  const [expandedSubscriptionId, setExpandedSubscriptionId] = useState(null);
  const [openMenuSubscriptionId, setOpenMenuSubscriptionId] = useState(null);

  const [authUser, setAuthUser] = useState(() => {
    const savedUser = localStorage.getItem("subtrack_user");
    return savedUser ? JSON.parse(savedUser) : null;
  });

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const [subscriptionForm, setSubscriptionForm] = useState(emptySubscriptionForm);

  const calculatedNextPaymentDate = calculateNextPaymentDate(
      subscriptionForm.startDate,
      subscriptionForm.billingCycle
  );

  const filteredSubscriptions = subscriptions.filter((subscription) => {
    const searchValue = searchText.toLowerCase();

    return (
        subscription.name?.toLowerCase().includes(searchValue) ||
        subscription.provider?.toLowerCase().includes(searchValue) ||
        subscription.categoryName?.toLowerCase().includes(searchValue)
    );
  });

  async function loadSubscriptions() {
    // Fetch subscriptions for the authenticated user.
    // The JWT token is automatically attached by the Axios interceptor.
    const subscriptionData = await getSubscriptions();
    setSubscriptions(subscriptionData);
  }

  async function loadReferenceData() {
    // Fetch global dropdown data used by the subscription modal.
    const [currencyData, categoryData, paymentMethodData] = await Promise.all([
      getCurrencies(),
      getCategories(),
      getPaymentMethods(),
    ]);

    setCurrencies(currencyData);
    setCategories(categoryData);
    setPaymentMethods(paymentMethodData);
  }

  useEffect(() => {
    async function loadHealth() {
      try {
        const healthData = await getBackendHealth();
        setBackendStatus(healthData.status);
      } catch {
        setBackendStatus("DOWN");
      }
    }

    // Explicitly ignore the returned promise to keep lint rules satisfied.
    void loadHealth();
  }, []);

  useEffect(() => {
    if (!authUser) {
      return;
    }

    // Load protected user data and global dropdown options after login.
    void Promise.all([loadSubscriptions(), loadReferenceData()]).catch(() => {
      setError("Could not load dashboard data.");
    });
  }, [authUser]);

  async function handleLogin(event) {
    event.preventDefault();
    setError("");

    try {
      const data = await login(email, password);

      // Store JWT and user info so the user stays logged in after refresh.
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
    } catch {
      setError("Login failed. Please check your email and password.");
    }
  }

  function handleLogout() {
    // Clear local auth data and return to the login screen.
    localStorage.removeItem("subtrack_token");
    localStorage.removeItem("subtrack_user");

    setAuthUser(null);
    setSubscriptions([]);
    setCurrencies([]);
    setCategories([]);
    setPaymentMethods([]);
    setExpandedSubscriptionId(null);
    setOpenMenuSubscriptionId(null);
    setEditingSubscription(null);
    setIsSubscriptionModalOpen(false);
    setError("");
  }

  function handleSubscriptionInputChange(event) {
    const { name, value } = event.target;

    setSubscriptionForm((currentValue) => ({
      ...currentValue,
      [name]: value,
    }));
  }

  function findCurrencyIdForSubscription(subscription) {
    // API response returns currencyCode, while the form needs currencyId.
    const matchedCurrency = currencies.find(
        (currency) => currency.code === subscription.currencyCode
    );

    return matchedCurrency?.id ? String(matchedCurrency.id) : "";
  }

  function findCategoryIdForSubscription(subscription) {
    // API response returns categoryName, while the form needs categoryId.
    const matchedCategory = categories.find(
        (category) => category.name === subscription.categoryName
    );

    return matchedCategory?.id ? String(matchedCategory.id) : "";
  }

  function findPaymentMethodIdForSubscription(subscription) {
    // API response returns paymentMethodName, while the form needs paymentMethodId.
    const matchedPaymentMethod = paymentMethods.find(
        (paymentMethod) => paymentMethod.name === subscription.paymentMethodName
    );

    return matchedPaymentMethod?.id ? String(matchedPaymentMethod.id) : "";
  }

  function openCreateModal() {
    setError("");
    setEditingSubscription(null);

    // Preselect the first available dropdown options to make the form easier to submit.
    setSubscriptionForm({
      ...emptySubscriptionForm,
      paidBy: authUser?.name || "",
      currencyId: currencies[0]?.id ? String(currencies[0].id) : "",
      categoryId: categories[0]?.id ? String(categories[0].id) : "",
      paymentMethodId: paymentMethods[0]?.id ? String(paymentMethods[0].id) : "",
    });

    setIsSubscriptionModalOpen(true);
  }

  function openEditModal(event, subscription) {
    // Open the same modal in edit mode and prefill it with the selected subscription.
    event.stopPropagation();
    setError("");
    setOpenMenuSubscriptionId(null);
    setEditingSubscription(subscription);

    setSubscriptionForm({
      name: subscription.name || "",
      paidBy: subscription.provider || "",
      price: subscription.price ? String(subscription.price) : "",
      billingCycle: subscription.billingCycle || "MONTHLY",
      startDate: subscription.startDate || "",
      currencyId: findCurrencyIdForSubscription(subscription),
      categoryId: findCategoryIdForSubscription(subscription),
      paymentMethodId: findPaymentMethodIdForSubscription(subscription),
      websiteUrl: subscription.websiteUrl || "",
      notes: subscription.notes || "",
    });

    setIsSubscriptionModalOpen(true);
  }

  function closeSubscriptionModal() {
    setIsSubscriptionModalOpen(false);
    setEditingSubscription(null);
    setSubscriptionForm(emptySubscriptionForm);
  }

  function toggleSubscriptionDetails(subscriptionId) {
    // Expands or collapses the detail row under a subscription.
    setExpandedSubscriptionId((currentId) =>
        currentId === subscriptionId ? null : subscriptionId
    );

    // Close the action menu when the row detail is toggled.
    setOpenMenuSubscriptionId(null);
  }

  function toggleSubscriptionMenu(event, subscriptionId) {
    // Prevent row click from also expanding/collapsing details.
    event.stopPropagation();

    setOpenMenuSubscriptionId((currentId) =>
        currentId === subscriptionId ? null : subscriptionId
    );
  }

  function handleClonePlaceholder(event) {
    // Clone is intentionally kept as a placeholder for a later feature.
    event.stopPropagation();
    setOpenMenuSubscriptionId(null);
    setError("Clone action is not implemented yet.");
  }

  async function handleDeleteSubscription(event, subscription) {
    // Delete is protected by the backend using subscription id + current JWT user id.
    event.stopPropagation();
    setOpenMenuSubscriptionId(null);
    setError("");

    const shouldDelete = window.confirm(
        `Are you sure you want to delete "${subscription.name}"?`
    );

    if (!shouldDelete) {
      return;
    }

    try {
      await deleteSubscription(subscription.id);

      if (expandedSubscriptionId === subscription.id) {
        setExpandedSubscriptionId(null);
      }

      await loadSubscriptions();
    } catch {
      setError("Could not delete subscription. Please try again.");
    }
  }

  function buildSubscriptionPayload() {
    const nextPaymentDate = calculateNextPaymentDate(
        subscriptionForm.startDate,
        subscriptionForm.billingCycle
    );

    if (!nextPaymentDate) {
      return null;
    }

    // Backend field is still named provider.
    // In the UI, we use it as "Paid by" until the backend field is renamed.
    return {
      name: subscriptionForm.name,
      provider: subscriptionForm.paidBy || null,
      price: Number(subscriptionForm.price),
      billingCycle: subscriptionForm.billingCycle,
      startDate: subscriptionForm.startDate,
      nextPaymentDate,
      autoRenew: true,
      notifyEnabled: true,
      notifyDaysBefore: 3,
      currencyId: Number(subscriptionForm.currencyId),
      categoryId: subscriptionForm.categoryId
          ? Number(subscriptionForm.categoryId)
          : null,
      paymentMethodId: subscriptionForm.paymentMethodId
          ? Number(subscriptionForm.paymentMethodId)
          : null,
      websiteUrl: subscriptionForm.websiteUrl || null,
      notes: subscriptionForm.notes || null,
    };
  }

  async function handleSubmitSubscription(event) {
    event.preventDefault();
    setError("");

    const payload = buildSubscriptionPayload();

    if (!payload) {
      setError("Please select a start date.");
      return;
    }

    try {
      if (editingSubscription) {
        await updateSubscription(editingSubscription.id, payload);
      } else {
        await createSubscription(payload);
      }

      closeSubscriptionModal();
      await loadSubscriptions();
    } catch {
      setError("Could not save subscription. Please check the form values.");
    }
  }

  function openWebsite(event, websiteUrl) {
    // Open website in a new tab only when the subscription has a valid URL.
    event.stopPropagation();

    if (!websiteUrl) {
      setError("This subscription does not have a website URL.");
      return;
    }

    window.open(websiteUrl, "_blank", "noopener,noreferrer");
  }

  if (!authUser) {
    return (
        <main className="st-auth-page">
          <section className="st-auth-card">
            <p className="st-eyebrow">Personal Finance Tracker</p>
            <h1>SubTrack</h1>
            <p className="st-auth-description">
              Track subscriptions, upcoming payments, and spending insights from
              one dashboard.
            </p>

            <div className="st-system-line">
              <span>Backend</span>
              <strong className={backendStatus === "UP" ? "st-up" : "st-down"}>
                {backendStatus}
              </strong>
            </div>

            <form className="st-login-form" onSubmit={handleLogin}>
              <div className="st-field">
                <label htmlFor="email">Email</label>
                <input
                    id="email"
                    type="email"
                    value={email}
                    placeholder="Enter your email"
                    onChange={(event) => setEmail(event.target.value)}
                />
              </div>

              <div className="st-field">
                <label htmlFor="password">Password</label>
                <input
                    id="password"
                    type="password"
                    value={password}
                    placeholder="Enter your password"
                    onChange={(event) => setPassword(event.target.value)}
                />
              </div>

              <button className="st-primary-button" type="submit">
                Login
              </button>
            </form>

            {error && <p className="st-error">{error}</p>}
          </section>
        </main>
    );
  }

  return (
      <main className="st-dashboard">
        <header className="st-topbar">
          <div className="st-logo">
            <span className="st-logo-mark">S</span>
            <span>SubTrack</span>
          </div>

          <div className="st-user-menu">
            <span className="st-avatar">{authUser.name?.charAt(0) || "U"}</span>
            <span>{authUser.name}</span>
            <button type="button" onClick={handleLogout}>
              Logout
            </button>
          </div>
        </header>

        <section className="st-toolbar">
          <button
              className="st-primary-button"
              type="button"
              onClick={openCreateModal}
          >
            + New Subscription
          </button>

          <div className="st-search-box">
            <input
                type="text"
                value={searchText}
                placeholder="Search"
                onChange={(event) => setSearchText(event.target.value)}
            />
            <span>⌕</span>
          </div>

          <button className="st-icon-button" type="button">
            ⌄
          </button>

          <button className="st-icon-button" type="button">
            ≡
          </button>
        </section>

        {error && <p className="st-error st-dashboard-error">{error}</p>}

        <section className="st-subscription-list">
          {filteredSubscriptions.length === 0 ? (
              <div className="st-empty-state">
                <h2>No subscriptions found</h2>
                <p>Create your first subscription using the button above.</p>
              </div>
          ) : (
              filteredSubscriptions.map((subscription) => (
                  <div className="st-subscription-item" key={subscription.id}>
                    <article
                        className="st-subscription-row"
                        onClick={() => toggleSubscriptionDetails(subscription.id)}
                    >
                      <div className="st-brand-placeholder">
                        {subscription.name?.charAt(0) || "S"}
                      </div>

                      <div className="st-subscription-name">
                        <strong>{subscription.name}</strong>
                        <span>{subscription.provider || "No payer"}</span>
                      </div>

                      <div className="st-subscription-cycle">
                        ↻ {subscription.billingCycle?.toLowerCase()}
                      </div>

                      <div className="st-subscription-date">
                        {formatDate(subscription.nextPaymentDate)}
                      </div>

                      <div className="st-subscription-price">
                        <strong>
                          {subscription.currencySymbol}
                          {subscription.price}
                        </strong>
                        <span>{subscription.categoryName || "No category"}</span>
                      </div>

                      <div className="st-row-actions">
                        <button
                            className="st-row-menu"
                            type="button"
                            onClick={(event) =>
                                toggleSubscriptionMenu(event, subscription.id)
                            }
                        >
                          ⋮
                        </button>

                        {openMenuSubscriptionId === subscription.id && (
                            <div className="st-action-menu">
                              <button
                                  type="button"
                                  onClick={(event) => openEditModal(event, subscription)}
                              >
                                ✎ Edit subscription
                              </button>

                              <button
                                  type="button"
                                  onClick={(event) =>
                                      handleDeleteSubscription(event, subscription)
                                  }
                              >
                                🗑 Delete
                              </button>

                              <button type="button" onClick={handleClonePlaceholder}>
                                ⧉ Clone
                              </button>
                            </div>
                        )}
                      </div>
                    </article>

                    {expandedSubscriptionId === subscription.id && (
                        <div className="st-subscription-details">
                          <div>
                            <span>Paid by</span>
                            <strong>{subscription.provider || "No payer"}</strong>
                          </div>

                          <div>
                            <span>Category</span>
                            <strong>{subscription.categoryName || "No category"}</strong>
                          </div>

                          <div>
                            <span>Payment Method</span>
                            <strong>
                              {subscription.paymentMethodName || "No payment method"}
                            </strong>
                          </div>

                          <div>
                            <span>Notes</span>
                            <strong>{subscription.notes || "No notes"}</strong>
                          </div>

                          <div>
                            <span>Website</span>
                            <button
                                className="st-detail-website-button"
                                type="button"
                                onClick={(event) =>
                                    openWebsite(event, subscription.websiteUrl)
                                }
                            >
                              🌐
                            </button>
                          </div>
                        </div>
                    )}
                  </div>
              ))
          )}
        </section>

        {isSubscriptionModalOpen && (
            <div className="st-modal-backdrop">
              <section className="st-modal">
                <header className="st-modal-header">
                  <h2>
                    {editingSubscription ? "Edit subscription" : "Add subscription"}
                  </h2>
                  <button type="button" onClick={closeSubscriptionModal}>
                    ×
                  </button>
                </header>

                <form className="st-modal-form" onSubmit={handleSubmitSubscription}>
                  <div className="st-full-field">
                    <input
                        name="name"
                        type="text"
                        value={subscriptionForm.name}
                        placeholder="Subscription name"
                        required
                        onChange={handleSubscriptionInputChange}
                    />
                  </div>

                  <div className="st-form-grid">
                    <input
                        name="price"
                        type="number"
                        step="0.01"
                        min="0.01"
                        value={subscriptionForm.price}
                        placeholder="Price"
                        required
                        onChange={handleSubscriptionInputChange}
                    />

                    <select
                        name="currencyId"
                        value={subscriptionForm.currencyId}
                        required
                        onChange={handleSubscriptionInputChange}
                    >
                      <option value="">Select currency</option>
                      {currencies.map((currency) => (
                          <option key={currency.id} value={currency.id}>
                            {currency.code} - {currency.symbol}
                          </option>
                      ))}
                    </select>
                  </div>

                  <div className="st-form-grid">
                    <input
                        name="paidBy"
                        type="text"
                        value={subscriptionForm.paidBy}
                        placeholder="Paid by (optional)"
                        onChange={handleSubscriptionInputChange}
                    />

                    <select
                        name="billingCycle"
                        value={subscriptionForm.billingCycle}
                        required
                        onChange={handleSubscriptionInputChange}
                    >
                      <option value="MONTHLY">Monthly</option>
                      <option value="YEARLY">Yearly</option>
                      <option value="WEEKLY">Weekly</option>
                    </select>
                  </div>

                  <div className="st-form-grid">
                    <select
                        name="categoryId"
                        value={subscriptionForm.categoryId}
                        onChange={handleSubscriptionInputChange}
                    >
                      <option value="">Select category</option>
                      {categories.map((category) => (
                          <option key={category.id} value={category.id}>
                            {category.name}
                          </option>
                      ))}
                    </select>

                    <select
                        name="paymentMethodId"
                        value={subscriptionForm.paymentMethodId}
                        onChange={handleSubscriptionInputChange}
                    >
                      <option value="">Select payment method</option>
                      {paymentMethods.map((paymentMethod) => (
                          <option key={paymentMethod.id} value={paymentMethod.id}>
                            {paymentMethod.name}
                          </option>
                      ))}
                    </select>
                  </div>

                  <div className="st-form-grid">
                    <label>
                      Start Date
                      <input
                          name="startDate"
                          type="date"
                          value={subscriptionForm.startDate}
                          required
                          onChange={handleSubscriptionInputChange}
                      />
                    </label>

                    <label>
                      Next Payment
                      <input
                          type="text"
                          value={
                            calculatedNextPaymentDate
                                ? formatDate(calculatedNextPaymentDate)
                                : "Select start date"
                          }
                          readOnly
                      />
                    </label>
                  </div>

                  <div className="st-full-field">
                    <input
                        name="websiteUrl"
                        type="text"
                        value={subscriptionForm.websiteUrl}
                        placeholder="Website URL, example: netflix.com"
                        onChange={handleSubscriptionInputChange}
                    />
                  </div>

                  <div className="st-full-field">
                    <input
                        name="notes"
                        type="text"
                        value={subscriptionForm.notes}
                        placeholder="Notes"
                        onChange={handleSubscriptionInputChange}
                    />
                  </div>

                  <div className="st-modal-actions">
                    <button
                        className="st-secondary-button"
                        type="button"
                        onClick={closeSubscriptionModal}
                    >
                      Cancel
                    </button>

                    <button className="st-primary-button" type="submit">
                      {editingSubscription ? "Save changes" : "Save"}
                    </button>
                  </div>
                </form>
              </section>
            </div>
        )}
      </main>
  );
}

export default App;