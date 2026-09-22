import apiClient from "./client";

export async function getNeglectInsights() {
  const response = await apiClient.get("/api/neglect");
  return response.data;
}
