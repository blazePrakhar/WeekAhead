import apiClient from "./client";

export async function getAnalytics(weeks = 12) {
  const response = await apiClient.get("/api/analytics", {
    params: {
      weeks,
    },
  });

  return response.data;
}
