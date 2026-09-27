import apiClient from "./client";

export async function generateWeeklyInsight(weekId) {
  const response = await apiClient.post(`/api/weeks/${weekId}/ai-insight`);

  return response.data;
}
