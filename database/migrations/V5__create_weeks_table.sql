CREATE TABLE weeks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    week_start_date DATE NOT NULL,
    available_minutes INT NOT NULL,
    fixed_commitment_minutes INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    CONSTRAINT fk_weeks_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_weeks_available_minutes
        CHECK (available_minutes >= 0),

    CONSTRAINT chk_weeks_fixed_commitment_minutes
        CHECK (fixed_commitment_minutes >= 0),

    CONSTRAINT chk_weeks_commitment_within_available
        CHECK (fixed_commitment_minutes <= available_minutes),

    CONSTRAINT uk_weeks_user_start_date
        UNIQUE (user_id, week_start_date)
);

CREATE INDEX idx_weeks_user_id
    ON weeks(user_id);