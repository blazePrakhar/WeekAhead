CREATE TABLE time_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    life_area_id BIGINT NOT NULL,
    log_date DATE NOT NULL,
    duration_minutes INT NOT NULL,
    note VARCHAR(500),
    source VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    CONSTRAINT fk_time_logs_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT fk_time_logs_life_area
        FOREIGN KEY (life_area_id)
        REFERENCES life_areas(id),

    CONSTRAINT chk_time_logs_duration
        CHECK (duration_minutes > 0)
);

CREATE INDEX idx_time_logs_user_date
    ON time_logs(user_id, log_date);

CREATE INDEX idx_time_logs_life_area_date
    ON time_logs(life_area_id, log_date);