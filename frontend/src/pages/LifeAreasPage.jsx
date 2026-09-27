import { useEffect, useState } from "react";
import { Layers3 } from "lucide-react";
import { motion } from "motion/react";
import { useForm } from "react-hook-form";

import {
  getLifeAreas,
  createLifeArea,
  updateLifeArea,
  deleteLifeArea,
} from "../api/lifeAreaApi";

const defaultValues = {
  name: "",
  description: "",
  weight: "",
  minMinutes: "",
  maxMinutes: "",
};

function LifeAreasPage() {
  const [lifeAreas, setLifeAreas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [editingId, setEditingId] = useState(null);

  const {
    register,
    handleSubmit,
    reset,
    getValues,
    formState: { errors },
  } = useForm({
    defaultValues,
  });

  useEffect(() => {
    let cancelled = false;

    const loadInitialLifeAreas = async () => {
      try {
        const data = await getLifeAreas();

        if (!cancelled) {
          setLifeAreas(data);
        }
      } catch (err) {
        if (!cancelled) {
          setError(
            err.response?.data?.message ||
              "Failed to load life areas. Please try again.",
          );
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    loadInitialLifeAreas();

    return () => {
      cancelled = true;
    };
  }, []);

  const loadLifeAreas = async () => {
    try {
      setError("");

      const data = await getLifeAreas();
      setLifeAreas(data);
    } catch (err) {
      setError(
        err.response?.data?.message ||
          "Failed to load life areas. Please try again.",
      );
    }
  };

  const handleFormSubmit = async (formData) => {
    try {
      setSubmitting(true);
      setError("");
      setSuccess("");

      const payload = {
        name: formData.name.trim(),
        description: formData.description.trim(),
        weight: Number(formData.weight),
        minMinutes: Number(formData.minMinutes),
        maxMinutes: Number(formData.maxMinutes),
      };

      if (editingId) {
        await updateLifeArea(editingId, payload);
        setSuccess("Life area updated successfully.");
      } else {
        await createLifeArea(payload);
        setSuccess("Life area created successfully.");
      }

      reset(defaultValues);
      setEditingId(null);

      await loadLifeAreas();
    } catch (err) {
      setError(
        err.response?.data?.message ||
          "Failed to save life area. Please check your input.",
      );
    } finally {
      setSubmitting(false);
    }
  };

  const handleEdit = (lifeArea) => {
    setEditingId(lifeArea.id);
    setError("");
    setSuccess("");

    reset({
      name: lifeArea.name ?? "",
      description: lifeArea.description ?? "",
      weight: lifeArea.weight ?? "",
      minMinutes: lifeArea.minMinutes ?? "",
      maxMinutes: lifeArea.maxMinutes ?? "",
    });

    window.scrollTo({
      top: 0,
      behavior: "smooth",
    });
  };

  const handleCancelEdit = () => {
    setEditingId(null);
    reset(defaultValues);
    setError("");
    setSuccess("");
  };

  const handleArchive = async (id) => {
    const confirmed = window.confirm(
      "Are you sure you want to archive this life area?",
    );

    if (!confirmed) {
      return;
    }

    try {
      setError("");
      setSuccess("");

      await deleteLifeArea(id);

      if (editingId === id) {
        setEditingId(null);
        reset(defaultValues);
      }

      setSuccess("Life area archived successfully.");
      await loadLifeAreas();
    } catch (err) {
      setError(
        err.response?.data?.message ||
          "Failed to archive life area. Please try again.",
      );
    }
  };

  if (loading) {
    return (
      <main className="life-areas-page">
        <section className="life-areas-header">
          <div>
            <h1>Life Areas</h1>
            <p className="page-description">
              Define the areas of your life and configure their importance and
              time constraints.
            </p>
          </div>
        </section>

        <div className="life-areas-status">
          <p>Loading life areas...</p>
        </div>
      </main>
    );
  }

  return (
    <motion.main
      className="life-areas-page"
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{
        duration: 0.3,
        ease: [0.2, 0.8, 0.2, 1],
      }}
    >
      <section className="life-areas-header">
        <div>
          <div className="life-areas-title-row">
            <span className="life-areas-page-icon" aria-hidden="true">
              <Layers3 size={22} strokeWidth={1.8} />
            </span>

            <div>
              <p className="life-areas-eyebrow">Your foundation</p>

              <h1>Life Areas</h1>
            </div>
          </div>

          <p className="page-description">
            Define the areas of your life and configure their importance and
            time constraints.
          </p>
        </div>

        {lifeAreas.length > 0 && (
          <div className="life-areas-count">
            <strong>{lifeAreas.length}</strong>
            <span>areas</span>
          </div>
        )}
      </section>

      {success && (
        <motion.div
          className="life-areas-success"
          initial={{ opacity: 0, y: -6 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.2 }}
        >
          <span className="life-areas-feedback-icon" aria-hidden="true">
            ✓
          </span>

          <p>{success}</p>
        </motion.div>
      )}

      <motion.section
        className={`life-area-form-card ${
          editingId ? "life-area-form-card-editing" : ""
        }`}
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{
          duration: 0.35,
          delay: 0.05,
          ease: [0.2, 0.8, 0.2, 1],
        }}
      >
        <div className="life-area-form-header">
          <div>
            <p className="life-area-form-eyebrow">
              {editingId ? "Updating your plan" : "Build your week"}
            </p>

            <h2>{editingId ? "Edit Life Area" : "Create Life Area"}</h2>

            <p>
              Set the importance and time constraints for this area of your
              life.
            </p>
          </div>

          <span className="life-area-form-symbol" aria-hidden="true">
            {editingId ? "↗" : "+"}
          </span>
        </div>

        <form onSubmit={handleSubmit(handleFormSubmit)} noValidate>
          <div className="form-group">
            <label htmlFor="name">Name</label>

            <input
              id="name"
              type="text"
              placeholder="e.g. Work, Fitness, Learning"
              {...register("name", {
                required: "Name is required.",
                maxLength: {
                  value: 100,
                  message: "Name must not exceed 100 characters.",
                },
              })}
            />

            {errors.name && <p className="form-error">{errors.name.message}</p>}
          </div>

          <div className="form-group">
            <label htmlFor="description">Description</label>

            <textarea
              id="description"
              rows="3"
              placeholder="Describe this life area..."
              {...register("description", {
                maxLength: {
                  value: 500,
                  message: "Description must not exceed 500 characters.",
                },
              })}
            />

            {errors.description && (
              <p className="form-error">{errors.description.message}</p>
            )}
          </div>

          <div className="form-row">
            <div className="form-group">
              <label htmlFor="weight">Importance Weight</label>

              <input
                id="weight"
                type="number"
                step="any"
                min="0"
                placeholder="e.g. 5"
                {...register("weight", {
                  required: "Importance weight is required.",
                  validate: (value) =>
                    Number(value) > 0 ||
                    "Importance weight must be greater than 0.",
                })}
              />

              {errors.weight && (
                <p className="form-error">{errors.weight.message}</p>
              )}
            </div>

            <div className="form-group">
              <label htmlFor="minMinutes">Minimum Minutes</label>

              <input
                id="minMinutes"
                type="number"
                min="0"
                step="1"
                placeholder="e.g. 120"
                {...register("minMinutes", {
                  required: "Minimum minutes is required.",
                  validate: (value) => {
                    const number = Number(value);

                    if (!Number.isInteger(number)) {
                      return "Minimum minutes must be a whole number.";
                    }

                    if (number < 0) {
                      return "Minimum minutes cannot be negative.";
                    }

                    return true;
                  },
                })}
              />

              {errors.minMinutes && (
                <p className="form-error">{errors.minMinutes.message}</p>
              )}
            </div>

            <div className="form-group">
              <label htmlFor="maxMinutes">Maximum Minutes</label>

              <input
                id="maxMinutes"
                type="number"
                min="0"
                step="1"
                placeholder="e.g. 600"
                {...register("maxMinutes", {
                  required: "Maximum minutes is required.",
                  validate: (value) => {
                    const number = Number(value);
                    const minMinutes = Number(getValues("minMinutes"));

                    if (!Number.isInteger(number)) {
                      return "Maximum minutes must be a whole number.";
                    }

                    if (number < 0) {
                      return "Maximum minutes cannot be negative.";
                    }

                    if (number < minMinutes) {
                      return "Maximum minutes must be greater than or equal to minimum minutes.";
                    }

                    return true;
                  },
                })}
              />

              {errors.maxMinutes && (
                <p className="form-error">{errors.maxMinutes.message}</p>
              )}
            </div>
          </div>

          {error && (
            <motion.p
              className="form-error life-areas-form-error"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
            >
              {error}
            </motion.p>
          )}

          <div className="form-actions">
            <motion.button
              type="submit"
              className="primary-button"
              disabled={submitting}
              whileTap={{ scale: 0.98 }}
            >
              {submitting
                ? "Saving..."
                : editingId
                  ? "Update Life Area"
                  : "Create Life Area"}
            </motion.button>

            {editingId && (
              <motion.button
                type="button"
                className="secondary-button"
                onClick={handleCancelEdit}
                disabled={submitting}
                whileTap={{ scale: 0.98 }}
              >
                Cancel
              </motion.button>
            )}
          </div>
        </form>
      </motion.section>

      <section className="life-areas-section">
        <div className="section-header">
          <div>
            <p className="life-areas-section-eyebrow">Your structure</p>
            <h2>Your Life Areas</h2>
          </div>

          {lifeAreas.length > 0 && (
            <span className="life-areas-section-count">
              {lifeAreas.length} configured
            </span>
          )}
        </div>

        {lifeAreas.length === 0 ? (
          <motion.div
            className="empty-state"
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.25 }}
          >
            <div className="life-areas-empty-icon" aria-hidden="true">
              <Layers3 size={22} strokeWidth={1.8} />
            </div>

            <h3>No life areas yet</h3>

            <p>
              Create your first life area above to start building your weekly
              plan.
            </p>
          </motion.div>
        ) : (
          <div className="life-areas-grid">
            {lifeAreas.map((lifeArea, index) => (
              <motion.article
                className={`life-area-card ${
                  lifeArea.isActive === false ? "life-area-card-archived" : ""
                }`}
                key={lifeArea.id}
                initial={{ opacity: 0, y: 12 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{
                  duration: 0.3,
                  delay: Math.min(index * 0.05, 0.25),
                  ease: [0.2, 0.8, 0.2, 1],
                }}
                whileHover={
                  lifeArea.isActive !== false
                    ? {
                        y: -3,
                        transition: { duration: 0.18 },
                      }
                    : undefined
                }
              >
                <div className="life-area-card-header">
                  <div className="life-area-card-title">
                    <span className="life-area-card-icon" aria-hidden="true">
                      {lifeArea.isActive === false ? (
                        "—"
                      ) : (
                        <Layers3 size={17} strokeWidth={1.8} />
                      )}
                    </span>

                    <div>
                      <h3>{lifeArea.name}</h3>

                      {lifeArea.description && (
                        <p className="life-area-description">
                          {lifeArea.description}
                        </p>
                      )}
                    </div>
                  </div>

                  {lifeArea.isActive === false && (
                    <span className="status-badge">Archived</span>
                  )}
                </div>

                <div className="life-area-details">
                  <div className="life-area-detail life-area-detail-primary">
                    <span className="detail-label">Importance</span>

                    <strong>{lifeArea.weight}</strong>
                  </div>

                  <div className="life-area-detail">
                    <span className="detail-label">Minimum</span>
                    <strong>{lifeArea.minMinutes} min</strong>
                  </div>

                  <div className="life-area-detail">
                    <span className="detail-label">Maximum</span>
                    <strong>{lifeArea.maxMinutes} min</strong>
                  </div>
                </div>

                <div className="life-area-actions">
                  <motion.button
                    type="button"
                    className="secondary-button"
                    onClick={() => handleEdit(lifeArea)}
                    disabled={lifeArea.isActive === false}
                    whileTap={{ scale: 0.98 }}
                  >
                    Edit
                  </motion.button>

                  {lifeArea.isActive !== false && (
                    <motion.button
                      type="button"
                      className="danger-button"
                      onClick={() => handleArchive(lifeArea.id)}
                      whileTap={{ scale: 0.98 }}
                    >
                      Archive
                    </motion.button>
                  )}
                </div>
              </motion.article>
            ))}
          </div>
        )}
      </section>
    </motion.main>
  );
}

export default LifeAreasPage;
