import apiClient from "./client";

export async function getLifeAreas() {
  const response = await apiClient.get("/api/life-areas");
  return response.data;
}

export async function getLifeArea(id) {
  const response = await apiClient.get(`/api/life-areas/${id}`);
  return response.data;
}

export async function createLifeArea(lifeArea) {
  const response = await apiClient.post("/api/life-areas", lifeArea);
  return response.data;
}

export async function updateLifeArea(id, lifeArea) {
  const response = await apiClient.put(`/api/life-areas/${id}`, lifeArea);
  return response.data;
}

export async function deleteLifeArea(id) {
  const response = await apiClient.delete(`/api/life-areas/${id}`);
  return response.data;
}
