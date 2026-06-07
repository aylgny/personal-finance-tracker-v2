import { api } from "./api";

export async function getCurrencies() {
    // Fetches the global currency list used by the subscription form dropdown.
    const response = await api.get("/api/currencies");
    return response.data;
}

export async function getCategories() {
    // Fetches the global category list used by the subscription form dropdown.
    const response = await api.get("/api/categories");
    return response.data;
}

export async function getPaymentMethods() {
    // Fetches the global payment method list used by the subscription form dropdown.
    const response = await api.get("/api/payment-methods");
    return response.data;
}