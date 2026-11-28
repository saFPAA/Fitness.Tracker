CREATE TABLE workout_set_creation_requests (
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    request_id UUID NOT NULL,
    workout_exercise_id BIGINT NOT NULL,
    set_number INTEGER NOT NULL,
    reps_done INTEGER NOT NULL,
    weight NUMERIC NOT NULL,
    set_id BIGINT,
    PRIMARY KEY (user_id, request_id)
);
