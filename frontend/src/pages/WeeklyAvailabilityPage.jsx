import { CalendarClock, Timer } from "lucide-react";
import { motion } from "motion/react";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";

import { createWeek, getCurrentWeek } from "../api/weekApi";

const defaultValues = {
  weekStartDate: "",
  availableHours: "",
  availableMinutes: "",
  fixedCommitmentHours: "",
  fixedCommitmentMinutes: "",
};

function minutesToHoursAndMinutes(totalMinutes) {
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;

  return {
    hours,
    minutes,
  };
}

function WeeklyAvailabilityPage() {
  const [week, setWeek] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const {
    register,
    handleSubmit,
    reset,
    watch,
    getValues,
    formState: { errors },
  } = useForm({
    defaultValues,
  });

  const availableHours = Number(watch("availableHours") || 0);
  const availableMinutes = Number(watch("availableMinutes") || 0);
  const fixedCommitmentHours = Number(watch("fixedCommitmentHours") || 0);
  const fixedCommitmentMinutes = Number(watch("fixedCommitmentMinutes") || 0);

  const totalAvailableMinutes = availableHours * 60 + availableMinutes;

  const totalFixedCommitmentMinutes =
    fixedCommitmentHours * 60 + fixedCommitmentMinutes;

  const discretionaryMinutes =
    totalAvailableMinutes - totalFixedCommitmentMinutes;

  useEffect(() => {
    let cancelled = false;

    const loadCurrentWeek = async () => {
      try {
        const data = await getCurrentWeek();

        if (cancelled) {
          return;
        }

        setWeek(data);

        const available = minutesToHoursAndMinutes(data.availableMinutes);

        const fixedCommitment = minutesToHoursAndMinutes(
          data.fixedCommitmentMinutes,
        );

        reset({
          weekStartDate: data.weekStartDate,
          availableHours: available.hours,
          availableMinutes: available.minutes,
          fixedCommitmentHours: fixedCommitment.hours,
          fixedCommitmentMinutes: fixedCommitment.minutes,
        });
      } catch (err) {
        if (!cancelled) {
          const message = err.response?.data?.message;

          if (message === "Current week not found") {
            setWeek(null);
            reset({
              ...defaultValues,
              weekStartDate: getMondayDate(),
            });
          } else {
            setError(
              message || "Failed to load the current week. Please try again.",
            );
          }
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    loadCurrentWeek();

    return () => {
      cancelled = true;
    };
  }, [reset]);

  const handleFormSubmit = async (formData) => {
    try {
      setSubmitting(true);
      setError("");
      setSuccess("");

      const availableMinutes =
        Number(formData.availableHours) * 60 +
        Number(formData.availableMinutes);

      const fixedCommitmentMinutes =
        Number(formData.fixedCommitmentHours) * 60 +
        Number(formData.fixedCommitmentMinutes);

      const payload = {
        weekStartDate: formData.weekStartDate,
        availableMinutes,
        fixedCommitmentMinutes,
      };

      const data = await createWeek(payload);

      setWeek(data);

      const available = minutesToHoursAndMinutes(data.availableMinutes);

      const fixedCommitment = minutesToHoursAndMinutes(
        data.fixedCommitmentMinutes,
      );

      reset({
        weekStartDate: data.weekStartDate,
        availableHours: available.hours,
        availableMinutes: available.minutes,
        fixedCommitmentHours: fixedCommitment.hours,
        fixedCommitmentMinutes: fixedCommitment.minutes,
      });

      setSuccess("Weekly availability saved successfully.");
    } catch (err) {
      const validationErrors = err.response?.data;

      if (
        validationErrors &&
        typeof validationErrors === "object" &&
        !validationErrors.message
      ) {
        setError(Object.values(validationErrors).join(" "));
      } else {
        setError(
          validationErrors?.message ||
            "Failed to save weekly availability. Please check your input.",
        );
      }
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <main className="weekly-availability-page">
        <section className="weekly-availability-header">
          <div>
            <h1>Weekly Availability</h1>
            <p className="page-description">
              Configure the time available for your week and your fixed
              commitments.
            </p>
          </div>
        </section>

        <div className="weekly-availability-status">
          <p>Loading weekly availability...</p>
        </div>
      </main>
    );
  }

  return (
    <motion.main
      className="weekly-availability-page"
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{
        duration: 0.3,
        ease: [0.2, 0.8, 0.2, 1],
      }}
    >
      <section className="weekly-availability-header">
        <div className="weekly-availability-title-row">
          <span className="weekly-availability-page-icon" aria-hidden="true">
            <CalendarClock size={22} strokeWidth={1.8} />
          </span>

          <div>
            <p className="weekly-availability-eyebrow">Shape your week</p>

            <h1>Weekly Availability</h1>

            <p className="page-description">
              Configure the time available for your week and your fixed
              commitments.
            </p>
          </div>
        </div>

        <div className="weekly-availability-week-badge">
          <span>{week ? "Current week" : "New week"}</span>
        </div>
      </section>

      {success && (
        <motion.div
          className="weekly-availability-success"
          initial={{ opacity: 0, y: -6 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.2 }}
        >
          <span
            className="weekly-availability-feedback-icon"
            aria-hidden="true"
          >
            ✓
          </span>

          <p>{success}</p>
        </motion.div>
      )}

      {error && (
        <motion.div
          className="weekly-availability-error"
          initial={{ opacity: 0, y: -6 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.2 }}
        >
          <span
            className="weekly-availability-feedback-icon"
            aria-hidden="true"
          >
            !
          </span>

          <p>{error}</p>
        </motion.div>
      )}

      <motion.section
        className="weekly-availability-card"
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{
          duration: 0.35,
          delay: 0.05,
          ease: [0.2, 0.8, 0.2, 1],
        }}
      >
        <div className="weekly-availability-form-header">
          <div>
            <p className="weekly-availability-form-eyebrow">
              {week ? "Current planning window" : "Weekly setup"}
            </p>

            <h2>{week ? "Current Week" : "Set Up Your Week"}</h2>

            <p>
              Enter your total available weekly time and the time already
              committed to fixed responsibilities.
            </p>
          </div>

          <span className="weekly-availability-form-symbol" aria-hidden="true">
            ◷
          </span>
        </div>

        <form onSubmit={handleSubmit(handleFormSubmit)} noValidate>
          <div className="form-group">
            <label htmlFor="weekStartDate">Week Start Date</label>

            <input
              id="weekStartDate"
              type="date"
              {...register("weekStartDate", {
                required: "Week start date is required.",
              })}
            />

            {errors.weekStartDate && (
              <p className="form-error">{errors.weekStartDate.message}</p>
            )}
          </div>

          <div className="weekly-availability-input-section">
            <div className="weekly-availability-input-section-header">
              <div>
                <span className="weekly-availability-input-icon">
                  <CalendarClock size={16} strokeWidth={1.8} />
                </span>

                <div>
                  <h3>Available Time</h3>
                  <p>Time you can allocate during the week.</p>
                </div>
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label htmlFor="availableHours">Hours</label>

                <input
                  id="availableHours"
                  type="number"
                  min="0"
                  step="1"
                  placeholder="e.g. 40"
                  {...register("availableHours", {
                    required: "Available hours are required.",
                    min: {
                      value: 0,
                      message: "Available hours cannot be negative.",
                    },
                    validate: (value) =>
                      Number.isInteger(Number(value)) ||
                      "Available hours must be a whole number.",
                  })}
                />

                {errors.availableHours && (
                  <p className="form-error">{errors.availableHours.message}</p>
                )}
              </div>

              <div className="form-group">
                <label htmlFor="availableMinutes">Minutes</label>

                <input
                  id="availableMinutes"
                  type="number"
                  min="0"
                  max="59"
                  step="1"
                  placeholder="e.g. 30"
                  {...register("availableMinutes", {
                    required: "Available minutes are required.",
                    min: {
                      value: 0,
                      message: "Minutes cannot be negative.",
                    },
                    max: {
                      value: 59,
                      message: "Minutes must be between 0 and 59.",
                    },
                    validate: (value) =>
                      Number.isInteger(Number(value)) ||
                      "Minutes must be a whole number.",
                  })}
                />

                {errors.availableMinutes && (
                  <p className="form-error">
                    {errors.availableMinutes.message}
                  </p>
                )}
              </div>
            </div>
          </div>

          <div className="weekly-availability-input-section">
            <div className="weekly-availability-input-section-header">
              <div>
                <span className="weekly-availability-input-icon">
                  <Timer size={16} strokeWidth={1.8} />
                </span>

                <div>
                  <h3>Fixed Commitments</h3>
                  <p>Time already committed to responsibilities.</p>
                </div>
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label htmlFor="fixedCommitmentHours">Hours</label>

                <input
                  id="fixedCommitmentHours"
                  type="number"
                  min="0"
                  step="1"
                  placeholder="e.g. 20"
                  {...register("fixedCommitmentHours", {
                    required: "Fixed commitment hours are required.",
                    min: {
                      value: 0,
                      message: "Fixed commitment hours cannot be negative.",
                    },
                    validate: (value) =>
                      Number.isInteger(Number(value)) ||
                      "Fixed commitment hours must be a whole number.",
                  })}
                />

                {errors.fixedCommitmentHours && (
                  <p className="form-error">
                    {errors.fixedCommitmentHours.message}
                  </p>
                )}
              </div>

              <div className="form-group">
                <label htmlFor="fixedCommitmentMinutes">Minutes</label>

                <input
                  id="fixedCommitmentMinutes"
                  type="number"
                  min="0"
                  max="59"
                  step="1"
                  placeholder="e.g. 30"
                  {...register("fixedCommitmentMinutes", {
                    required: "Fixed commitment minutes are required.",
                    min: {
                      value: 0,
                      message: "Minutes cannot be negative.",
                    },
                    max: {
                      value: 59,
                      message: "Minutes must be between 0 and 59.",
                    },
                    validate: (value) => {
                      if (!Number.isInteger(Number(value))) {
                        return "Minutes must be a whole number.";
                      }

                      const availableTotalMinutes =
                        Number(getValues("availableHours") || 0) * 60 +
                        Number(getValues("availableMinutes") || 0);

                      const fixedTotalMinutes =
                        Number(getValues("fixedCommitmentHours") || 0) * 60 +
                        Number(value);

                      return (
                        fixedTotalMinutes <= availableTotalMinutes ||
                        "Fixed commitments cannot exceed available time."
                      );
                    },
                  })}
                />

                {errors.fixedCommitmentMinutes && (
                  <p className="form-error">
                    {errors.fixedCommitmentMinutes.message}
                  </p>
                )}
              </div>
            </div>
          </div>

          <div className="weekly-availability-summary">
            <div className="weekly-summary-card weekly-summary-available">
              <span>Available Time</span>

              <strong>
                {Math.floor(totalAvailableMinutes / 60)}h{" "}
                {totalAvailableMinutes % 60}m
              </strong>

              <small>Total weekly capacity</small>
            </div>

            <div className="weekly-summary-card weekly-summary-fixed">
              <span>Fixed Commitments</span>

              <strong>
                {Math.floor(totalFixedCommitmentMinutes / 60)}h{" "}
                {totalFixedCommitmentMinutes % 60}m
              </strong>

              <small>Already committed</small>
            </div>

            <div
              className={`weekly-summary-card weekly-summary-discretionary ${
                discretionaryMinutes < 0 ? "weekly-summary-negative" : ""
              }`}
            >
              <span>Discretionary Time</span>

              <strong>
                {Math.floor(discretionaryMinutes / 60)}h{" "}
                {Math.abs(discretionaryMinutes % 60)}m
              </strong>

              <small>Available for planning</small>
            </div>
          </div>

          <div className="form-actions">
            <motion.button
              type="submit"
              className="primary-button"
              disabled={submitting}
              whileTap={{ scale: 0.98 }}
            >
              {submitting ? "Saving..." : "Save Availability"}
            </motion.button>
          </div>
        </form>
      </motion.section>
    </motion.main>
  );
}

function getMondayDate() {
  const today = new Date();
  const day = today.getDay();
  const difference = day === 0 ? -6 : 1 - day;

  today.setDate(today.getDate() + difference);

  return today.toISOString().split("T")[0];
}

export default WeeklyAvailabilityPage;
