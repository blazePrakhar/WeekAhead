import { useState } from "react";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { loginUser } from "../api/authApi";
import { useAuth } from "../context/useAuth";

function LoginPage() {
  const navigate = useNavigate();
  const { login } = useAuth();

  const [serverError, setServerError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm({
    defaultValues: {
      email: "",
      password: "",
    },
  });

  const onSubmit = async (data) => {
    setServerError("");
    setSuccessMessage("");

    try {
      const response = await loginUser({
        email: data.email.trim().toLowerCase(),
        password: data.password,
      });

      login(response.accessToken);

      setSuccessMessage("Login successful.");
    } catch (error) {
      if (error.response?.status === 401) {
        setServerError("Invalid email or password.");
      } else if (error.response?.status === 400) {
        setServerError(
          error.response.data?.message || "Please check your login details.",
        );
      } else {
        setServerError(
          error.response?.data?.message || "Login failed. Please try again.",
        );
      }
    }
  };

  return (
    <main className="app-shell">
      <section className="foundation-card">
        <p className="eyebrow">WeekAhead</p>

        <h1>Welcome back</h1>

        <p>Login to continue planning your week.</p>

        <form onSubmit={handleSubmit(onSubmit)} noValidate>
          <div>
            <label htmlFor="email">Email</label>

            <input
              id="email"
              type="email"
              autoComplete="email"
              {...register("email", {
                required: "Email is required.",
                pattern: {
                  value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
                  message: "Enter a valid email address.",
                },
              })}
            />

            {errors.email && <p>{errors.email.message}</p>}
          </div>

          <div>
            <label htmlFor="password">Password</label>

            <input
              id="password"
              type="password"
              autoComplete="current-password"
              {...register("password", {
                required: "Password is required.",
                minLength: {
                  value: 8,
                  message: "Password must be at least 8 characters.",
                },
              })}
            />

            {errors.password && <p>{errors.password.message}</p>}
          </div>

          {serverError && <p>{serverError}</p>}
          {successMessage && <p>{successMessage}</p>}

          <button type="submit" disabled={isSubmitting}>
            {isSubmitting ? "Logging in..." : "Login"}
          </button>
        </form>

        <p>
          Don't have an account?{" "}
          <button type="button" onClick={() => navigate("/register")}>
            Register
          </button>
        </p>
      </section>
    </main>
  );
}

export default LoginPage;
