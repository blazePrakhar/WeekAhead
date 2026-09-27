import { ArrowRight, CheckCircle2, Clock3, Sparkles } from "lucide-react";
import { motion } from "motion/react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";

import { loginUser } from "../api/authApi";
import { useAuth } from "../context/useAuth";

function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();

  const [serverError, setServerError] = useState("");

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

    try {
      const response = await loginUser({
        email: data.email.trim().toLowerCase(),
        password: data.password,
      });

      login(response.accessToken);

      navigate("/life-areas");
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
    <main className="auth-page">
      <div className="auth-shell">
        <motion.section
          className="auth-intro"
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3, ease: "easeOut" }}
        >
          <div className="auth-brand">
            <span className="auth-brand-mark" aria-hidden="true">
              <Sparkles size={17} strokeWidth={2} />
            </span>

            <span>WeekAhead</span>
          </div>

          <div className="auth-intro-copy">
            <p className="auth-eyebrow">YOUR WEEK, WITH INTENTION</p>

            <h1>Make room for what matters.</h1>

            <p>
              Plan your available time, protect your priorities, and understand
              where your week is really going.
            </p>
          </div>

          <div className="auth-feature-list">
            <div className="auth-feature">
              <span className="auth-feature-icon" aria-hidden="true">
                <Clock3 size={17} strokeWidth={2} />
              </span>

              <div>
                <strong>Plan with your real time</strong>
                <span>Build a weekly budget around your availability.</span>
              </div>
            </div>

            <div className="auth-feature">
              <span className="auth-feature-icon" aria-hidden="true">
                <CheckCircle2 size={17} strokeWidth={2} />
              </span>

              <div>
                <strong>Stay aligned</strong>
                <span>See how your actual week compares with your plan.</span>
              </div>
            </div>
          </div>
        </motion.section>

        <motion.section
          className="auth-card"
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.35, delay: 0.05, ease: "easeOut" }}
        >
          <div className="auth-card-header">
            <p className="auth-card-kicker">WELCOME BACK</p>

            <h2>Sign in to WeekAhead</h2>

            <p>Continue planning your week with clarity.</p>
          </div>

          <form
            className="auth-form"
            onSubmit={handleSubmit(onSubmit)}
            noValidate
          >
            <div className="auth-field">
              <label htmlFor="email">Email</label>

              <input
                id="email"
                type="email"
                autoComplete="email"
                placeholder="you@example.com"
                aria-invalid={errors.email ? "true" : "false"}
                {...register("email", {
                  required: "Email is required.",
                  pattern: {
                    value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
                    message: "Enter a valid email address.",
                  },
                })}
              />

              {errors.email && (
                <p className="auth-field-error" role="alert">
                  {errors.email.message}
                </p>
              )}
            </div>

            <div className="auth-field">
              <div className="auth-field-label-row">
                <label htmlFor="password">Password</label>
              </div>

              <input
                id="password"
                type="password"
                autoComplete="current-password"
                placeholder="Enter your password"
                aria-invalid={errors.password ? "true" : "false"}
                {...register("password", {
                  required: "Password is required.",
                  minLength: {
                    value: 8,
                    message: "Password must be at least 8 characters.",
                  },
                })}
              />

              {errors.password && (
                <p className="auth-field-error" role="alert">
                  {errors.password.message}
                </p>
              )}
            </div>

            {serverError && (
              <div className="auth-server-error" role="alert">
                <span className="auth-server-error-dot" aria-hidden="true" />
                <span>{serverError}</span>
              </div>
            )}

            <motion.button
              className="auth-submit"
              type="submit"
              disabled={isSubmitting}
              whileHover={!isSubmitting ? { y: -1 } : undefined}
              whileTap={!isSubmitting ? { scale: 0.99 } : undefined}
            >
              <span>{isSubmitting ? "Logging in..." : "Login"}</span>

              {!isSubmitting && (
                <ArrowRight size={18} strokeWidth={2} aria-hidden="true" />
              )}
            </motion.button>
          </form>

          <div className="auth-divider">
            <span>New to WeekAhead?</span>
          </div>

          <p className="auth-footer">
            Create an account to start building a more intentional week.
          </p>

          <button
            className="auth-secondary-action"
            type="button"
            onClick={() => navigate("/register")}
          >
            <span>Register</span>
            <ArrowRight size={16} strokeWidth={2} aria-hidden="true" />
          </button>
        </motion.section>
      </div>
    </main>
  );
}

export default LoginPage;
