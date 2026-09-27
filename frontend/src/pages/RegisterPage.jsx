import { ArrowRight, CheckCircle2, Clock3, Sparkles } from "lucide-react";
import { motion } from "motion/react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";

import { registerUser } from "../api/authApi";
import { getApiErrorMessage } from "../api/errorHandler";

function RegisterPage() {
  const navigate = useNavigate();

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
      await registerUser({
        email: data.email.trim().toLowerCase(),
        password: data.password,
      });

      setSuccessMessage("Registration successful. Redirecting to login...");

      setTimeout(() => {
        navigate("/login");
      }, 1000);
    } catch (error) {
      if (error.response?.status === 409) {
        setServerError("An account with this email already exists.");
      } else if (error.response?.status === 400) {
        setServerError(
          getApiErrorMessage(error, "Please check your registration details."),
        );
      } else {
        setServerError(
          getApiErrorMessage(error, "Registration failed. Please try again."),
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
            <p className="auth-eyebrow">START WITH YOUR WEEK</p>

            <h1>Build a week that works for you.</h1>

            <p>
              Set up your priorities, understand your available time, and turn
              your weekly intentions into a practical plan.
            </p>
          </div>

          <div className="auth-feature-list">
            <div className="auth-feature">
              <span className="auth-feature-icon" aria-hidden="true">
                <Clock3 size={17} strokeWidth={2} />
              </span>

              <div>
                <strong>Start with your availability</strong>
                <span>
                  Give your week a realistic time budget from the beginning.
                </span>
              </div>
            </div>

            <div className="auth-feature">
              <span className="auth-feature-icon" aria-hidden="true">
                <CheckCircle2 size={17} strokeWidth={2} />
              </span>

              <div>
                <strong>Keep priorities visible</strong>
                <span>
                  Organize the life areas that deserve your attention.
                </span>
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
            <p className="auth-card-kicker">GET STARTED</p>

            <h2>Create your WeekAhead account</h2>

            <p>Set up your account and start planning with intention.</p>
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
              <label htmlFor="password">Password</label>

              <input
                id="password"
                type="password"
                autoComplete="new-password"
                placeholder="Create a password"
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

              {!errors.password && (
                <p className="auth-field-hint">
                  Use at least 8 characters for your password.
                </p>
              )}
            </div>

            {serverError && (
              <div className="auth-server-error" role="alert">
                <span className="auth-server-error-dot" aria-hidden="true" />
                <span>{serverError}</span>
              </div>
            )}

            {successMessage && (
              <div className="auth-success" role="status">
                <CheckCircle2 size={16} strokeWidth={2} aria-hidden="true" />
                <span>{successMessage}</span>
              </div>
            )}

            <motion.button
              className="auth-submit"
              type="submit"
              disabled={isSubmitting}
              whileHover={!isSubmitting ? { y: -1 } : undefined}
              whileTap={!isSubmitting ? { scale: 0.99 } : undefined}
            >
              <span>
                {isSubmitting ? "Creating account..." : "Create account"}
              </span>

              {!isSubmitting && (
                <ArrowRight size={18} strokeWidth={2} aria-hidden="true" />
              )}
            </motion.button>
          </form>

          <div className="auth-divider">
            <span>Already using WeekAhead?</span>
          </div>

          <p className="auth-footer">
            Sign in to continue planning and balancing your week.
          </p>

          <button
            className="auth-secondary-action"
            type="button"
            onClick={() => navigate("/login")}
          >
            <span>Login</span>
            <ArrowRight size={16} strokeWidth={2} aria-hidden="true" />
          </button>
        </motion.section>
      </div>
    </main>
  );
}

export default RegisterPage;
