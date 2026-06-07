import { api } from "./api";

export async function getSubscriptions() {
  // Fetches subscriptions for the authenticated user.
  // The JWT token is added automatically by the Axios interceptor.
  const response = await api.get("/api/subscriptions");
  return response.data;
}

export async function createSubscription(subscriptionData) {
  // Creates a new subscription for the authenticated user.
  // The backend determines the user from the JWT token, not from the request body.
  const response = await api.post("/api/subscriptions", subscriptionData);
  return response.data;
}