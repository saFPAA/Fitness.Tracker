-- удаление тренировки удаляет её упражнения и подходы
ALTER TABLE workouts DROP CONSTRAINT workouts_user_id_fkey,
  ADD CONSTRAINT workouts_user_id_fkey FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
ALTER TABLE workout_exercises DROP CONSTRAINT workout_exercises_workout_id_fkey,
  ADD CONSTRAINT workout_exercises_workout_id_fkey FOREIGN KEY (workout_id) REFERENCES workouts (id) ON DELETE CASCADE;
ALTER TABLE workout_sets DROP CONSTRAINT workout_sets_workout_exercise_id_fkey,
  ADD CONSTRAINT workout_sets_workout_exercise_id_fkey FOREIGN KEY (workout_exercise_id) REFERENCES workout_exercises (id) ON DELETE CASCADE;


INSERT INTO exercises (name, muscle_group, equipment) VALUES
                                                          ('Жим лёжа', 'Грудь', 'Штанга'),
                                                          ('Приседания со штангой', 'Ноги', 'Штанга'),
                                                          ('Становая тяга', 'Спина', 'Штанга'),
                                                          ('Подтягивания', 'Спина', 'Турник'),
                                                          ('Жим стоя', 'Плечи', 'Штанга'),
                                                          ('Сгибание рук на бицепс', 'Руки', 'Гантели');