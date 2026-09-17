import apiClient from "./client";

export async function registerUser(credentials) {
  const response = await apiClient.post("/api/auth/register", credentials);

  return response.data;
}
