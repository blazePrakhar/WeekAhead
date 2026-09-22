import apiClient from "./client";

export async function getRebalancingSuggestions() {
  const response = await apiClient.get("/api/rebalancing");
  return response.data;
}
