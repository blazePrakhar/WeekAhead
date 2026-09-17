import { useState } from "react";
import { AuthContext } from "./AuthContext.js";
import {
  clearAccessToken,
  getAccessToken,
  setAccessToken,
} from "../api/tokenStore";

export function AuthProvider({ children }) {
  const [accessToken, setAccessTokenState] = useState(getAccessToken());

  const login = (token) => {
    setAccessToken(token);
    setAccessTokenState(token);
  };

  const logout = () => {
    clearAccessToken();
    setAccessTokenState(null);
  };

  const isAuthenticated = Boolean(accessToken);

  return (
    <AuthContext.Provider
      value={{
        accessToken,
        isAuthenticated,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}
