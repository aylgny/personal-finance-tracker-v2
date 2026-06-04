import { api } from "./api";

export async function getSubscriptions() {
  const response = await api.get("/api/subscriptions");
  return response.data;
}