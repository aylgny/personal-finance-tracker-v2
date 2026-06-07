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

export async function updateSubscription(subscriptionId, subscriptionData) {
  // Updates an existing subscription owned by the authenticated user.
  // The backend checks ownership using subscription id + JWT user id.
  const response = await api.put(`/api/subscriptions/${subscriptionId}`, subscriptionData);
  return response.data;
}

export async function deleteSubscription(subscriptionId) {
  // Deletes an existing subscription owned by the authenticated user.
  // Successful delete returns 204 No Content.
  await api.delete(`/api/subscriptions/${subscriptionId}`);
}