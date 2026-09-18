import { useState } from "react";
import { AuthContext } from "./AuthContext.js";
import {
  clearAccessToken,
  getAccessToken,
  setAccessToken,
} from "../api/tokenStore";

export function AuthProvider({ children }) {
  const [accessToken, setAccessTokenState] = useState(getAccessToken());
  const [currentUser, setCurrentUser] = useState(null);

  const login = (token) => {
    setAccessToken(token);
    setAccessTokenState(token);
  };

  const updateCurrentUser = (user) => {
    setCurrentUser(user);
  };

  const logout = () => {
    clearAccessToken();
    setAccessTokenState(null);
    setCurrentUser(null);
  };

  const isAuthenticated = Boolean(accessToken);

  return (
    <AuthContext.Provider
      value={{
        accessToken,
        currentUser,
        isAuthenticated,
        login,
        updateCurrentUser,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}
