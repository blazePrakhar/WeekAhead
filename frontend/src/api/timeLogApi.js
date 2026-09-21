import apiClient from "./client";

export async function createTimeLog(timeLog) {
  const response = await apiClient.post("/api/time-logs", timeLog);
  return response.data;
}

export async function getTimeLogs(from, to, lifeAreaId) {
  const params = {
    from,
    to,
  };

  if (lifeAreaId !== undefined && lifeAreaId !== null) {
    params.lifeAreaId = lifeAreaId;
  }

  const response = await apiClient.get("/api/time-logs", {
    params,
  });

  return response.data;
}

export async function updateTimeLog(id, timeLog) {
  const response = await apiClient.put(`/api/time-logs/${id}`, timeLog);
  return response.data;
}

export async function deleteTimeLog(id) {
  const response = await apiClient.delete(`/api/time-logs/${id}`);
  return response.data;
}
