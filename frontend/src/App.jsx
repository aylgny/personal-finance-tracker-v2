/* eslint-disable react-hooks/set-state-in-effect */

import { useEffect, useState } from "react";
import { getBackendHealth } from "./services/healthService";
import {
  activateSubscription,
  createSubscription,
  deleteSubscription,
  disableSubscription,
  getSubscriptions,
  updateSubscription,
} from "./services/subscriptionService";
import { login } from "./services/authService";
import {
  getCategories,
  getCurrencies,
  getPaymentMethods,
} from "./services/referenceDataService";
import { getAiRecommendations } from "./services/aiService";
import "./calendar.css";

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
  disabled: false,
};

const calendarWeekdays = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];

function parseDateValue(dateValue) {
  // Backend usually returns LocalDate as YYYY-MM-DD.
  // This helper avoids timezone shifts and also supports normal Date-compatible values.
  if (!dateValue) {
    return null;
  }

  if (typeof dateValue === "string" && /^\d{4}-\d{2}-\d{2}/.test(dateValue)) {
    const [year, month, day] = dateValue.slice(0, 10).split("-").map(Number);
    return new Date(year, month - 1, day);
  }

  const parsedDate = new Date(dateValue);

  if (Number.isNaN(parsedDate.getTime())) {
    return null;
  }

  return parsedDate;
}

function formatDate(dateValue) {
  const parsedDate = parseDateValue(dateValue);

  if (!parsedDate) {
    return "-";
  }

  return parsedDate.toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
  });
}

function calculateNextPaymentDate(startDate, billingCycle) {
  // Calculates the next payment date from the selected start date and billing cycle.
  if (!startDate) {
    return "";
  }

  const date = parseDateValue(startDate);

  if (!date) {
    return "";
  }

  if (billingCycle === "WEEKLY") {
    date.setDate(date.getDate() + 7);
  }

  if (billingCycle === "MONTHLY") {
    date.setMonth(date.getMonth() + 1);
  }

  if (billingCycle === "YEARLY") {
    date.setFullYear(date.getFullYear() + 1);
  }

  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

function formatMoney(symbol, amount) {
  // Keeps money formatting consistent across dashboard/statistics cards.
  return `${symbol || ""}${Number(amount || 0).toFixed(2)}`;
}

function getMonthlyEquivalent(subscription) {
  // Converts different billing cycles into an estimated monthly value.
  const price = Number(subscription.price || 0);

  if (subscription.billingCycle === "WEEKLY") {
    return (price * 52) / 12;
  }

  if (subscription.billingCycle === "YEARLY") {
    return price / 12;
  }

  return price;
}

function getTotalsByCurrency(subscriptions) {
  // Groups cost totals by currency because TRY, USD, and EUR should not be mixed.
  return subscriptions.reduce((totals, subscription) => {
    const currencyCode = subscription.currencyCode || "UNKNOWN";
    const currencySymbol = subscription.currencySymbol || "";
    const monthlyAmount = getMonthlyEquivalent(subscription);

    if (!totals[currencyCode]) {
      totals[currencyCode] = {
        currencyCode,
        currencySymbol,
        monthlyTotal: 0,
        yearlyTotal: 0,
        count: 0,
      };
    }

    totals[currencyCode].monthlyTotal += monthlyAmount;
    totals[currencyCode].yearlyTotal += monthlyAmount * 12;
    totals[currencyCode].count += 1;

    return totals;
  }, {});
}

function getChargeTotalsByCurrency(paymentItems) {
  // Sums exact payment amounts by currency for calendar statistics.
  // Each item represents one payment occurrence.
  return paymentItems.reduce((totals, paymentItem) => {
    const currencyCode = paymentItem.currencyCode || "UNKNOWN";
    const currencySymbol = paymentItem.currencySymbol || "";
    const amount = Number(paymentItem.price || 0);

    if (!totals[currencyCode]) {
      totals[currencyCode] = {
        currencyCode,
        currencySymbol,
        total: 0,
        count: 0,
      };
    }

    totals[currencyCode].total += amount;
    totals[currencyCode].count += 1;

    return totals;
  }, {});
}

function formatTotalsByCurrency(currencyRows, fieldName) {
  // Displays grouped totals in one dashboard/statistics card.
  // Example: ₺120.00 / $20.00
  if (currencyRows.length === 0) {
    return "-";
  }

  return currencyRows
      .map((currencyTotal) =>
          formatMoney(currencyTotal.currencySymbol, currencyTotal[fieldName])
      )
      .join(" / ");
}

function isInDisplayedMonth(dateValue, monthDate) {
  const parsedDate = parseDateValue(dateValue);

  if (!parsedDate) {
    return false;
  }

  return (
      parsedDate.getFullYear() === monthDate.getFullYear() &&
      parsedDate.getMonth() === monthDate.getMonth()
  );
}

function getLastDayOfMonth(monthDate) {
  // Returns the last day number of the selected month.
  return new Date(monthDate.getFullYear(), monthDate.getMonth() + 1, 0).getDate();
}

function getStartOfToday() {
  // Used for "Amount Due This Month" so past days in the current month are ignored.
  const today = new Date();
  return new Date(today.getFullYear(), today.getMonth(), today.getDate());
}

function isSameMonth(firstDate, secondDate) {
  return (
      firstDate.getFullYear() === secondDate.getFullYear() &&
      firstDate.getMonth() === secondDate.getMonth()
  );
}

function isTodayCell(dayNumber, selectedMonth) {
  // Highlights today's day only when the calendar is showing the current month.
  const today = getStartOfToday();

  return (
      dayNumber === today.getDate() &&
      selectedMonth.getFullYear() === today.getFullYear() &&
      selectedMonth.getMonth() === today.getMonth()
  );
}

function getSubscriptionOccurrenceDatesInMonth(subscription, monthDate) {
  // Calculates which exact dates a subscription should appear on in the selected month.
  // Monthly subscriptions repeat every month on the same payment day.
  // Yearly subscriptions appear only in the matching month.
  // Weekly subscriptions repeat every 7 days.
  const baseDate = parseDateValue(subscription.nextPaymentDate);

  if (!baseDate) {
    return [];
  }

  const year = monthDate.getFullYear();
  const month = monthDate.getMonth();
  const daysInMonth = getLastDayOfMonth(monthDate);
  const occurrenceDates = [];

  if (subscription.billingCycle === "MONTHLY") {
    const preferredDay = baseDate.getDate();
    const occurrenceDay = Math.min(preferredDay, daysInMonth);

    occurrenceDates.push(new Date(year, month, occurrenceDay));

    return occurrenceDates;
  }

  if (subscription.billingCycle === "YEARLY") {
    if (baseDate.getMonth() === monthDate.getMonth()) {
      const occurrenceDay = Math.min(baseDate.getDate(), daysInMonth);
      occurrenceDates.push(new Date(year, month, occurrenceDay));
    }

    return occurrenceDates;
  }

  if (subscription.billingCycle === "WEEKLY") {
    const firstDayOfSelectedMonth = new Date(year, month, 1);
    const lastDayOfSelectedMonth = new Date(year, month, daysInMonth);
    const occurrenceDate = new Date(baseDate);

    while (occurrenceDate < firstDayOfSelectedMonth) {
      occurrenceDate.setDate(occurrenceDate.getDate() + 7);
    }

    while (occurrenceDate <= lastDayOfSelectedMonth) {
      occurrenceDates.push(new Date(occurrenceDate));
      occurrenceDate.setDate(occurrenceDate.getDate() + 7);
    }

    return occurrenceDates;
  }

  if (isInDisplayedMonth(subscription.nextPaymentDate, monthDate)) {
    occurrenceDates.push(baseDate);
  }

  return occurrenceDates;
}

function getSubscriptionOccurrenceDaysInMonth(subscription, monthDate) {
  // Converts occurrence dates into day numbers for the calendar cells.
  return getSubscriptionOccurrenceDatesInMonth(subscription, monthDate).map(
      (occurrenceDate) => occurrenceDate.getDate()
  );
}

function getRemainingPaymentItemsForSelectedMonth(subscriptions, selectedMonth) {
  // Calculates remaining payments in the selected month.
  // Current month: today and future days are included.
  // Future month: the whole month is included.
  // Past month: no remaining amount is included.
  const today = getStartOfToday();

  return subscriptions.flatMap((subscription) => {
    const occurrenceDates = getSubscriptionOccurrenceDatesInMonth(
        subscription,
        selectedMonth
    );

    return occurrenceDates
        .filter((occurrenceDate) => {
          if (selectedMonth < new Date(today.getFullYear(), today.getMonth(), 1)) {
            return false;
          }

          if (isSameMonth(selectedMonth, today)) {
            return occurrenceDate >= today;
          }

          return true;
        })
        .map((occurrenceDate) => ({
          ...subscription,
          occurrenceDate,
        }));
  });
}

function getCalendarCells(monthDate) {
  // Builds a 6x7 calendar grid using Monday as the first day of the week.
  const year = monthDate.getFullYear();
  const month = monthDate.getMonth();

  const firstDayOfMonth = new Date(year, month, 1);
  const daysInMonth = new Date(year, month + 1, 0).getDate();

  // Convert Sunday-first JS weekday to Monday-first index.
  const startOffset = (firstDayOfMonth.getDay() + 6) % 7;

  const cells = [];

  for (let index = 0; index < 42; index += 1) {
    const dayNumber = index - startOffset + 1;

    if (dayNumber < 1 || dayNumber > daysInMonth) {
      cells.push(null);
    } else {
      cells.push(dayNumber);
    }
  }

  return cells;
}

function getMonthLabel(monthDate) {
  return monthDate.toLocaleDateString("en-US", {
    month: "long",
    year: "numeric",
  });
}

function getRecommendationTitle(recommendation) {
  // Supports both the new object format and the old string format for safety.
  if (typeof recommendation === "string") {
    return recommendation;
  }

  return recommendation.title || "AI Recommendation";
}

function getRecommendationDescription(recommendation) {
  // Shows Gemini-generated explanation when the backend returns structured recommendations.
  if (typeof recommendation === "string") {
    return "This recommendation is generated from your subscription data.";
  }

  return (
      recommendation.description ||
      "This recommendation is generated from your subscription data."
  );
}

function getRecommendationSaving(recommendation) {
  // Saving is optional because not every recommendation has a direct cost saving.
  if (typeof recommendation === "string") {
    return null;
  }

  return recommendation.saving || null;
}

function getDomainFromWebsiteUrl(websiteUrl) {
  // Extracts the domain from a subscription website URL.
  // Supports both "netflix.com" and "https://netflix.com".
  if (!websiteUrl) {
    return "";
  }

  try {
    const normalizedUrl = websiteUrl.startsWith("http")
        ? websiteUrl
        : `https://${websiteUrl}`;

    const url = new URL(normalizedUrl);

    return url.hostname.replace(/^www\./, "");
  } catch {
    return "";
  }
}

function getFaviconUrl(websiteUrl) {
  // Uses Google's favicon service to get a small public logo for the domain.
  // If favicon loading fails, the component falls back to the first letter.
  const domain = getDomainFromWebsiteUrl(websiteUrl);

  if (!domain) {
    return "";
  }

  return `https://www.google.com/s2/favicons?domain=${domain}&sz=64`;
}

function SubscriptionLogo({ subscription, className = "" }) {
  const [hasLogoError, setHasLogoError] = useState(false);
  const faviconUrl = getFaviconUrl(subscription.websiteUrl);
  const fallbackLetter = subscription.name?.charAt(0) || "S";

  if (!faviconUrl || hasLogoError) {
    return (
        <div className={`st-subscription-logo-fallback ${className}`}>
          {fallbackLetter}
        </div>
    );
  }

  return (
      <div className={`st-subscription-logo-wrapper ${className}`}>
        <img
            src={faviconUrl}
            alt={`${subscription.name} logo`}
            onError={() => setHasLogoError(true)}
        />
      </div>
  );
}

function App() {
  const [backendStatus, setBackendStatus] = useState("Checking...");
  const [subscriptions, setSubscriptions] = useState([]);
  const [currencies, setCurrencies] = useState([]);
  const [categories, setCategories] = useState([]);
  const [paymentMethods, setPaymentMethods] = useState([]);
  const [error, setError] = useState("");
  const [searchText, setSearchText] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [categoryFilter, setCategoryFilter] = useState("ALL");
  const [currencyFilter, setCurrencyFilter] = useState("ALL");
  const [currentPage, setCurrentPage] = useState("dashboard");
  const [selectedMonth, setSelectedMonth] = useState(
      () => new Date(new Date().getFullYear(), new Date().getMonth(), 1)
  );
  const [isSubscriptionModalOpen, setIsSubscriptionModalOpen] = useState(false);
  const [editingSubscription, setEditingSubscription] = useState(null);
  const [expandedSubscriptionId, setExpandedSubscriptionId] = useState(null);
  const [openMenuSubscriptionId, setOpenMenuSubscriptionId] = useState(null);
  const [aiRecommendations, setAiRecommendations] = useState([]);
  const [isAiLoading, setIsAiLoading] = useState(false);
  const [aiError, setAiError] = useState("");
  const [hasLoadedAiRecommendations, setHasLoadedAiRecommendations] =
      useState(false);

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

  const activeSubscriptions = subscriptions.filter(
      (subscription) => subscription.status === "ACTIVE"
  );

  const inactiveSubscriptions = subscriptions.filter(
      (subscription) => subscription.status === "INACTIVE"
  );

  const upcomingPayments = [...activeSubscriptions]
      .filter((subscription) => subscription.nextPaymentDate)
      .sort((firstSubscription, secondSubscription) => {
        const firstDate = parseDateValue(firstSubscription.nextPaymentDate);
        const secondDate = parseDateValue(secondSubscription.nextPaymentDate);

        if (!firstDate || !secondDate) {
          return 0;
        }

        return firstDate - secondDate;
      })
      .slice(0, 4);

  const activeTotalsByCurrency = Object.values(
      getTotalsByCurrency(activeSubscriptions)
  );

  const inactiveTotalsByCurrency = Object.values(
      getTotalsByCurrency(inactiveSubscriptions)
  );

  const filteredSubscriptions = subscriptions.filter((subscription) => {
    const searchValue = searchText.toLowerCase();

    const matchesSearch =
        subscription.name?.toLowerCase().includes(searchValue) ||
        subscription.provider?.toLowerCase().includes(searchValue) ||
        subscription.categoryName?.toLowerCase().includes(searchValue);

    const matchesStatus =
        statusFilter === "ALL" || subscription.status === statusFilter;

    const matchesCategory =
        categoryFilter === "ALL" || subscription.categoryName === categoryFilter;

    const matchesCurrency =
        currencyFilter === "ALL" || subscription.currencyCode === currencyFilter;

    return matchesSearch && matchesStatus && matchesCategory && matchesCurrency;
  });

  const sortedFilteredSubscriptions = [...filteredSubscriptions].sort(
      (firstSubscription, secondSubscription) => {
        // Keep inactive subscriptions at the bottom of the subscription list.
        const firstIsInactive = firstSubscription.status === "INACTIVE";
        const secondIsInactive = secondSubscription.status === "INACTIVE";

        if (firstIsInactive && !secondIsInactive) {
          return 1;
        }

        if (!firstIsInactive && secondIsInactive) {
          return -1;
        }

        // Keep subscriptions with earlier payment dates higher within their own status group.
        const firstDate = parseDateValue(firstSubscription.nextPaymentDate);
        const secondDate = parseDateValue(secondSubscription.nextPaymentDate);

        if (!firstDate && !secondDate) {
          return 0;
        }

        if (!firstDate) {
          return 1;
        }

        if (!secondDate) {
          return -1;
        }

        return firstDate - secondDate;
      }
  );

  const calendarSubscriptions = [...activeSubscriptions]
      .filter(
          (subscription) =>
              getSubscriptionOccurrenceDaysInMonth(subscription, selectedMonth).length > 0
      )
      .sort((firstSubscription, secondSubscription) => {
        // Only active subscriptions are shown on the calendar.
        const firstOccurrenceDays = getSubscriptionOccurrenceDaysInMonth(
            firstSubscription,
            selectedMonth
        );

        const secondOccurrenceDays = getSubscriptionOccurrenceDaysInMonth(
            secondSubscription,
            selectedMonth
        );

        const firstDay = firstOccurrenceDays[0] || 999;
        const secondDay = secondOccurrenceDays[0] || 999;

        if (firstDay !== secondDay) {
          return firstDay - secondDay;
        }

        return (firstSubscription.name || "").localeCompare(
            secondSubscription.name || ""
        );
      });

  const calendarSubscriptionsByDay = calendarSubscriptions.reduce(
      (groupedSubscriptions, subscription) => {
        const occurrenceDays = getSubscriptionOccurrenceDaysInMonth(
            subscription,
            selectedMonth
        );

        occurrenceDays.forEach((dayNumber) => {
          if (!groupedSubscriptions[dayNumber]) {
            groupedSubscriptions[dayNumber] = [];
          }

          groupedSubscriptions[dayNumber].push(subscription);
        });

        return groupedSubscriptions;
      },
      {}
  );

  const remainingPaymentItemsForSelectedMonth =
      getRemainingPaymentItemsForSelectedMonth(activeSubscriptions, selectedMonth);

  const dueThisMonthTotalsByCurrency = Object.values(
      getChargeTotalsByCurrency(remainingPaymentItemsForSelectedMonth)
  );

  const calendarCells = getCalendarCells(selectedMonth);

  async function loadSubscriptions() {
    // Fetch subscriptions for the authenticated user.
    // The JWT token is automatically attached by the Axios interceptor.
    const subscriptionData = await getSubscriptions();
    setSubscriptions(subscriptionData);
  }

  async function loadReferenceData() {
    // Fetch global dropdown data used by the subscription modal and filters.
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

  async function handleGenerateAiRecommendations() {
    setIsAiLoading(true);
    setAiError("");

    try {
      const data = await getAiRecommendations();

      if (data.recommendations && data.recommendations.length > 0) {
        setAiRecommendations(data.recommendations);
      } else {
        setAiRecommendations([]);
      }

      setHasLoadedAiRecommendations(true);
    } catch {
      setAiError("Could not generate AI recommendations. Please try again.");
      setAiRecommendations([]);
      setHasLoadedAiRecommendations(true);
    } finally {
      setIsAiLoading(false);
    }
  }

  useEffect(() => {
    if (
        !authUser ||
        currentPage !== "dashboard" ||
        hasLoadedAiRecommendations ||
        isAiLoading
    ) {
      return;
    }

    // Automatically generate AI recommendations once when the dashboard is opened.
    void handleGenerateAiRecommendations();
  }, [authUser, currentPage, hasLoadedAiRecommendations, isAiLoading]);

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
    setCurrentPage("dashboard");
    setSearchText("");
    setStatusFilter("ALL");
    setCategoryFilter("ALL");
    setCurrencyFilter("ALL");
    setAiRecommendations([]);
    setAiError("");
    setHasLoadedAiRecommendations(false);
    setError("");
  }

  function resetSubscriptionFilters() {
    // Clears all subscription filters and search text.
    setSearchText("");
    setStatusFilter("ALL");
    setCategoryFilter("ALL");
    setCurrencyFilter("ALL");
  }

  function handleSubscriptionInputChange(event) {
    const { name, value, type, checked } = event.target;

    setSubscriptionForm((currentValue) => ({
      ...currentValue,
      [name]: type === "checkbox" ? checked : value,
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
      disabled: subscription.status === "INACTIVE",
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

      // Refresh AI insights after data changes so recommendations stay relevant.
      setHasLoadedAiRecommendations(false);
      setAiRecommendations([]);
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

  async function syncSubscriptionStatus(subscriptionId) {
    // Status is updated with dedicated endpoints instead of mixing it into the edit request.
    if (subscriptionForm.disabled) {
      await disableSubscription(subscriptionId);
      return;
    }

    await activateSubscription(subscriptionId);
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
        await syncSubscriptionStatus(editingSubscription.id);
      } else {
        await createSubscription(payload);
      }

      closeSubscriptionModal();
      await loadSubscriptions();

      // Refresh AI insights after data changes so recommendations stay relevant.
      setHasLoadedAiRecommendations(false);
      setAiRecommendations([]);
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

  function goToPreviousMonth() {
    setSelectedMonth(
        (currentMonth) =>
            new Date(currentMonth.getFullYear(), currentMonth.getMonth() - 1, 1)
    );
  }

  function goToNextMonth() {
    setSelectedMonth(
        (currentMonth) =>
            new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1, 1)
    );
  }

  function renderDashboard() {
    return (
        <section className="st-dashboard-page">
          <div className="st-dashboard-hero">
            <div>
              <p className="st-eyebrow">Dashboard</p>
              <h1>Hello {authUser.name}</h1>
              <p>
                Track upcoming payments, active subscriptions, savings, and monthly
                cost estimates from one place.
              </p>
            </div>

            <button
                className="st-primary-button"
                type="button"
                onClick={openCreateModal}
            >
              + New Subscription
            </button>
          </div>

          <section className="st-dashboard-section">
            <div className="st-section-heading">
              <h2>Upcoming Payments</h2>
              <button
                  className="st-text-button"
                  type="button"
                  onClick={() => setCurrentPage("subscriptions")}
              >
                View all
              </button>
            </div>

            {upcomingPayments.length === 0 ? (
                <div className="st-empty-state">
                  <h2>No upcoming payments</h2>
                  <p>Create active subscriptions to see upcoming payments here.</p>
                </div>
            ) : (
                <div className="st-upcoming-grid">
                  {upcomingPayments.map((subscription) => (
                      <article className="st-upcoming-card" key={subscription.id}>
                        <SubscriptionLogo
                            subscription={subscription}
                            className="st-upcoming-logo"
                        />
                        <strong>{subscription.name}</strong>
                        <span>{formatDate(subscription.nextPaymentDate)}</span>
                        <b>
                          {subscription.currencySymbol}
                          {subscription.price}
                        </b>
                      </article>
                  ))}
                </div>
            )}
          </section>

          <section className="st-dashboard-section">
            <h2>Your Subscriptions</h2>

            <div className="st-summary-grid">
              <article className="st-summary-card">
                <span>Active Subscriptions</span>
                <strong>{activeSubscriptions.length}</strong>
              </article>

              <article className="st-summary-card">
                <span>Monthly Cost</span>
                <strong>
                  {formatTotalsByCurrency(activeTotalsByCurrency, "monthlyTotal")}
                </strong>
              </article>

              <article className="st-summary-card">
                <span>Yearly Cost</span>
                <strong>
                  {formatTotalsByCurrency(activeTotalsByCurrency, "yearlyTotal")}
                </strong>
              </article>
            </div>
          </section>

          <section className="st-dashboard-section">
            <h2>Your Savings</h2>

            <div className="st-summary-grid">
              <article className="st-summary-card">
                <span>Inactive Subscriptions</span>
                <strong>{inactiveSubscriptions.length}</strong>
              </article>

              <article className="st-summary-card">
                <span>Monthly Savings</span>
                <strong>
                  {formatTotalsByCurrency(inactiveTotalsByCurrency, "monthlyTotal")}
                </strong>
              </article>

              <article className="st-summary-card">
                <span>Yearly Savings</span>
                <strong>
                  {formatTotalsByCurrency(inactiveTotalsByCurrency, "yearlyTotal")}
                </strong>
              </article>
            </div>
          </section>

          <section className="st-dashboard-section">
            <div className="st-section-heading">
              <h2>AI Recommendations</h2>
              {isAiLoading && <span className="st-ai-loading-text">Generating...</span>}
            </div>

            {aiError && <p className="st-error">{aiError}</p>}

            {aiRecommendations.length === 0 ? (
                <div className="st-empty-state">
                  <h2>
                    {isAiLoading
                        ? "Generating AI recommendations..."
                        : "No AI recommendations yet"}
                  </h2>
                  <p>
                    AI recommendations are generated automatically from your
                    subscription data.
                  </p>
                </div>
            ) : (
                <div className="st-recommendation-list">
                  {aiRecommendations.map((recommendation, index) => {
                    const title = getRecommendationTitle(recommendation);
                    const description = getRecommendationDescription(recommendation);
                    const saving = getRecommendationSaving(recommendation);

                    return (
                        <details key={`${title}-${index}`} open={index === 0}>
                          <summary>
                            {index + 1}. {title}
                          </summary>

                          <p>{description}</p>

                          {saving && <p className="st-ai-saving-text">{saving}</p>}
                        </details>
                    );
                  })}
                </div>
            )}
          </section>
        </section>
    );
  }

  function renderSubscriptionsPage() {
    return (
        <>
          <section className="st-toolbar st-toolbar-with-filters">
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

            <select
                className="st-filter-select"
                value={statusFilter}
                onChange={(event) => setStatusFilter(event.target.value)}
            >
              <option value="ALL">All statuses</option>
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
            </select>

            <select
                className="st-filter-select"
                value={categoryFilter}
                onChange={(event) => setCategoryFilter(event.target.value)}
            >
              <option value="ALL">All categories</option>
              {categories.map((category) => (
                  <option key={category.id} value={category.name}>
                    {category.name}
                  </option>
              ))}
            </select>

            <select
                className="st-filter-select"
                value={currencyFilter}
                onChange={(event) => setCurrencyFilter(event.target.value)}
            >
              <option value="ALL">All currencies</option>
              {currencies.map((currency) => (
                  <option key={currency.id} value={currency.code}>
                    {currency.code}
                  </option>
              ))}
            </select>

            <button
                className="st-secondary-button st-clear-filters-button"
                type="button"
                onClick={resetSubscriptionFilters}
            >
              Clear
            </button>
          </section>

          <section className="st-subscription-list">
            {sortedFilteredSubscriptions.length === 0 ? (
                <div className="st-empty-state">
                  <h2>No subscriptions found</h2>
                  <p>Try changing your search text or filters.</p>
                </div>
            ) : (
                sortedFilteredSubscriptions.map((subscription) => {
                  const isInactive = subscription.status === "INACTIVE";

                  return (
                      <div className="st-subscription-item" key={subscription.id}>
                        <article
                            className={`st-subscription-row ${
                                isInactive ? "st-subscription-row-inactive" : ""
                            }`}
                            onClick={() => toggleSubscriptionDetails(subscription.id)}
                        >
                          <SubscriptionLogo
                              subscription={subscription}
                              className="st-brand-placeholder"
                          />

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
                            <span>
                        {isInactive
                            ? "Inactive"
                            : subscription.categoryName || "No category"}
                      </span>
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
                                </div>
                            )}
                          </div>
                        </article>

                        {expandedSubscriptionId === subscription.id && (
                            <div className="st-subscription-details">
                              <div>
                                <span>Status</span>
                                <strong>{subscription.status}</strong>
                              </div>

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
                                <span>Website</span>
                                <button
                                    className="st-detail-website-button"
                                    type="button"
                                    onClick={(event) =>
                                        openWebsite(event, subscription.websiteUrl)
                                    }
                                >
                                  🌐 Open website
                                </button>
                              </div>
                            </div>
                        )}
                      </div>
                  );
                })
            )}
          </section>
        </>
    );
  }

  function renderCalendarPage() {
    return (
        <section className="st-dashboard-page st-calendar-page">
          <section className="st-calendar-section">
            <div className="st-calendar-header">
              <div className="st-calendar-title">
                <h2>Calendar</h2>
                <span>📅</span>
              </div>

              <div className="st-calendar-month-switcher">
                <button
                    className="st-calendar-nav-button"
                    type="button"
                    onClick={goToPreviousMonth}
                >
                  ‹
                </button>

                <strong>{getMonthLabel(selectedMonth)}</strong>

                <button
                    className="st-calendar-nav-button"
                    type="button"
                    onClick={goToNextMonth}
                >
                  ›
                </button>
              </div>
            </div>

            <div className="st-calendar-board">
              <div className="st-calendar-weekdays">
                {calendarWeekdays.map((weekday) => (
                    <div key={weekday}>{weekday}</div>
                ))}
              </div>

              <div className="st-calendar-grid">
                {calendarCells.map((dayNumber, index) => {
                  const subscriptionsForDay = dayNumber
                      ? calendarSubscriptionsByDay[dayNumber] || []
                      : [];

                  return (
                      <div
                          className={`st-calendar-cell ${
                              dayNumber ? "" : "st-calendar-cell-empty"
                          } ${
                              dayNumber && isTodayCell(dayNumber, selectedMonth)
                                  ? "st-calendar-today"
                                  : ""
                          }`}
                          key={`${dayNumber || "empty"}-${index}`}
                      >
                        {dayNumber ? (
                            <>
                              <div className="st-calendar-day-number">{dayNumber}</div>

                              <div className="st-calendar-events">
                                {subscriptionsForDay.map((subscription) => (
                                    <button
                                        className="st-calendar-event-pill"
                                        type="button"
                                        key={subscription.id}
                                        onClick={(event) =>
                                            openEditModal(event, subscription)
                                        }
                                        title={`${subscription.name} - ${subscription.currencySymbol}${subscription.price}`}
                                    >
                                      {subscription.name}
                                    </button>
                                ))}
                              </div>
                            </>
                        ) : null}
                      </div>
                  );
                })}
              </div>
            </div>
          </section>

          <section className="st-calendar-stats-section">
            <h3>Statistics</h3>

            <div className="st-calendar-stats-grid">
              <article className="st-calendar-stat-card">
                <strong>{activeSubscriptions.length}</strong>
                <span>Active Subscriptions</span>
              </article>

              <article className="st-calendar-stat-card">
                <strong>
                  {formatTotalsByCurrency(activeTotalsByCurrency, "monthlyTotal")}
                </strong>
                <span>Total Monthly Cost</span>
              </article>

              <article className="st-calendar-stat-card">
                <strong>
                  {formatTotalsByCurrency(dueThisMonthTotalsByCurrency, "total")}
                </strong>
                <span>Amount Due This Month</span>
              </article>
            </div>
          </section>
        </section>
    );
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

          <nav className="st-main-nav">
            <button
                className={currentPage === "dashboard" ? "st-nav-active" : ""}
                type="button"
                onClick={() => setCurrentPage("dashboard")}
            >
              Dashboard
            </button>

            <button
                className={currentPage === "subscriptions" ? "st-nav-active" : ""}
                type="button"
                onClick={() => setCurrentPage("subscriptions")}
            >
              Subscriptions
            </button>

            <button
                className={currentPage === "calendar" ? "st-nav-active" : ""}
                type="button"
                onClick={() => setCurrentPage("calendar")}
            >
              Calendar
            </button>
          </nav>

          <div className="st-user-menu">
            <span className="st-avatar">{authUser.name?.charAt(0) || "U"}</span>
            <span>{authUser.name}</span>
            <button type="button" onClick={handleLogout}>
              Logout
            </button>
          </div>
        </header>

        {error && <p className="st-error st-dashboard-error">{error}</p>}

        {currentPage === "dashboard" && renderDashboard()}
        {currentPage === "subscriptions" && renderSubscriptionsPage()}
        {currentPage === "calendar" && renderCalendarPage()}

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

                  {editingSubscription && (
                      <label className="st-switch-row">
                        <input
                            name="disabled"
                            type="checkbox"
                            checked={subscriptionForm.disabled}
                            onChange={handleSubscriptionInputChange}
                        />
                        <span className="st-switch-track">
                    <span className="st-switch-thumb" />
                  </span>
                        <span>Disable Subscription</span>
                      </label>
                  )}

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