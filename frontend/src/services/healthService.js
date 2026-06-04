import { api } from "./api";

export async function getBackendHealth() {
  const response = await api.get("/api/health");
  return response.data;
}