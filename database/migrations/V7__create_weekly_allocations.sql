CREATE TABLE weekly_allocations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    week_id BIGINT NOT NULL,
    life_area_id BIGINT NOT NULL,
    recommended_minutes INT NOT NULL,
    planned_minutes INT NOT NULL DEFAULT 0,
    actual_minutes INT NOT NULL DEFAULT 0,
    algorithm_version VARCHAR(50) NOT NULL,
    explanation VARCHAR(1000),

    PRIMARY KEY (id),

    CONSTRAINT fk_weekly_allocations_week
        FOREIGN KEY (week_id)
        REFERENCES weeks(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_weekly_allocations_life_area
        FOREIGN KEY (life_area_id)
        REFERENCES life_areas(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_weekly_allocations_recommended
        CHECK (recommended_minutes >= 0),

    CONSTRAINT chk_weekly_allocations_planned
        CHECK (planned_minutes >= 0),

    CONSTRAINT chk_weekly_allocations_actual
        CHECK (actual_minutes >= 0),

    CONSTRAINT uk_weekly_allocations_week_life_area
        UNIQUE (week_id, life_area_id)
);

CREATE INDEX idx_weekly_allocations_week_id
    ON weekly_allocations(week_id);

CREATE INDEX idx_weekly_allocations_life_area_id
    ON weekly_allocations(life_area_id);