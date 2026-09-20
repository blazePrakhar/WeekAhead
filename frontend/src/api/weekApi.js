import apiClient from "./client";

export async function getCurrentWeek() {
  const response = await apiClient.get("/api/weeks/current");
  return response.data;
}

export async function getWeek(id) {
  const response = await apiClient.get(`/api/weeks/${id}`);
  return response.data;
}

export async function createWeek(week) {
  const response = await apiClient.post("/api/weeks", week);
  return response.data;
}
