import { api } from "./api";

export async function login(email, password) {
    // Sends login credentials to the backend and returns the JWT response.
    const response = await api.post("/api/auth/login", {
        email,
        password,
    });

    return response.data;
}

export async function register(name, email, password) {
    // Sends registration data to the backend and returns the JWT response.
    const response = await api.post("/api/auth/register", {
        name,
        email,
        password,
    });

    return response.data;
}