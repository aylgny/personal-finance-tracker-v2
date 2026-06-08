import { api } from "./api";

export async function getAiRecommendations() {
    // Fetches AI-powered recommendations for the authenticated user.
    // The JWT token is automatically attached by the Axios interceptor.
    const response = await api.get("/api/ai/recommendations");
    return response.data;
}