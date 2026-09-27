const ACCESS_TOKEN_KEY = "weekahead_access_token";

export function setAccessToken(token) {
  sessionStorage.setItem(ACCESS_TOKEN_KEY, token);
}

export function getAccessToken() {
  return sessionStorage.getItem(ACCESS_TOKEN_KEY);
}

export function clearAccessToken() {
  sessionStorage.removeItem(ACCESS_TOKEN_KEY);
}
