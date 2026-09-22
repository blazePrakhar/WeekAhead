import apiClient from "./client";

export async function getWeeklyDashboard() {
  const response = await apiClient.get("/api/dashboard/weekly");
  return response.data;
}
