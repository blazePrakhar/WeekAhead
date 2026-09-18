import apiClient from "./client";

export async function registerUser(credentials) {
  const response = await apiClient.post("/api/auth/register", credentials);
  return response.data;
}

export async function loginUser(credentials) {
  const response = await apiClient.post("/api/auth/login", credentials);

  return response.data;
}

export async function getCurrentUser() {
  const response = await apiClient.get("/api/auth/me");
  return response.data;
}
