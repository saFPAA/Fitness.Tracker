# Fitness Tracker

PWA-приложение для ведения дневника тренировок и отслеживания прогресса. Пользователь создаёт тренировки, добавляет упражнения и записывает подходы; каталог упражнений можно фильтровать и пополнять.

## Возможности

- Регистрация и вход по логину и паролю.
- JWT access-токен и refresh-токен; выход и ротация refresh-токенов.
- Создание, редактирование и удаление тренировок.
- Каталог упражнений с поиском, фильтрацией по группе мышц, описанием и ссылками на видео и изображения.
- Запись упражнений и подходов в тренировке.
- История и показатели прогресса по упражнению.
- Роли `USER` и `ADMIN`; пользовательские данные доступны владельцу и администратору.
- Администратор может управлять общим каталогом упражнений.
- Ограничение частоты входа, регистрации и неудачной смены пароля через Redis.
- Устанавливаемое PWA с офлайн-очередью изменений подходов и синхронизацией при восстановлении связи.

Регистрация создаёт пользователя и сразу выполняет вход. Подтверждение адреса электронной почты, восстановление пароля по email и изменение логина или email сейчас не реализованы.

## Стек

| Компонент | Технологии |
| --- | --- |
| Backend | Java 21, Spring Boot 4, Spring MVC, Spring Data JPA, Spring Security, Bean Validation |
| База данных | PostgreSQL 17, Flyway |
| Ограничение запросов | Redis 7 |
| Авторизация | JWT, BCrypt, refresh-токены с хранением SHA-256 хэша |
| Frontend | React 19, TypeScript, Vite, React Router, TanStack Query, Tailwind CSS |
| Тесты | JUnit 5, Mockito, Spring Boot Test, Testcontainers, Vitest |
| Запуск | Maven Wrapper, npm, Docker Compose |

## Структура

```text
src/main/java/com/Author/Fitness/
├── Auth/             Регистрация, вход и управление токенами
├── Security/         JWT, правила доступа и Redis rate limiting
├── User/             Пользователи, профиль и смена пароля
├── Exercise/         Каталог упражнений
├── Workout/          Тренировки
├── WorkoutExercise/  Упражнения в тренировках
├── WorkoutSet/       Подходы
└── Tools/            Прогресс и служебные настройки

src/main/resources/db/migration/  Миграции Flyway
src/test/java/                    Backend-тесты
frontend/src/                     Страницы, компоненты, API и PWA
```

## Быстрый запуск через Docker Compose

Нужны Docker Engine и Docker Compose v2.

1. Создай в корне проекта файл `.env`:

   ```dotenv
   DB_USERNAME=fitness_db
   DB_PASSWORD=замени-на-сильный-пароль
   DB_URL=jdbc:postgresql://db:5432/fitness_db
   JWT_SECRET=вставь-длинный-случайный-секрет
   ```

2. Сгенерируй секрет JWT:

   ```bash
   openssl rand -base64 48
   ```

   Вставь результат в `JWT_SECRET`. Файл `.env` не следует добавлять в Git.

3. Из корня проекта собери и запусти приложение:

   ```bash
   docker compose up -d --build
   ```

Compose запустит PostgreSQL, Redis и приложение. Flyway применит миграции при старте. Frontend встроен в backend-образ, поэтому приложение будет доступно по адресу [http://localhost:8080](http://localhost:8080).

Проверить состояние контейнеров и посмотреть логи:

```bash
docker compose ps
docker compose logs -f app
```

Остановить приложение:

```bash
docker compose down
```

Данные PostgreSQL сохраняются в Docker-томе `pgdata`. Команда `docker compose down -v` удаляет этот том вместе с данными.

> Текущий Compose публикует приложение только на localhost. Это конфигурация для локального запуска, не для непосредственного публичного размещения в интернете.

## Локальная разработка

### Backend

Запусти PostgreSQL и Redis в контейнерах:

```bash
docker compose up -d db redis
```

Задай переменные из `.env` в конфигурации запуска IDE или терминале. Для backend, работающего на хосте, укажи адреса опубликованных сервисов:

```bash
export DB_URL=jdbc:postgresql://localhost:5433/fitness_db
export REDIS_HOST=localhost
export REDIS_PORT=6379
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Также должны быть заданы `DB_USERNAME`, `DB_PASSWORD` и `JWT_SECRET`. Backend запускается на порту `8080`.

### Frontend

Нужен Node.js 22 или новее. Установи зависимости и запусти Vite:

```bash
cd frontend
npm ci
npm run dev
```

Vite использует proxy для `/api` и отправляет запросы на backend по адресу `http://localhost:8080`.

## API

Все маршруты `/api/**`, кроме `/api/auth/**`, требуют access-токен:

```http
Authorization: Bearer <accessToken>
```

### Проверка через Swagger UI

Профиль `dev` открывает [Swagger UI](http://localhost:8080/swagger-ui.html) и [спецификацию OpenAPI](http://localhost:8080/v3/api-docs) на порту Spring Boot. При запуске из IDE задай `SPRING_PROFILES_ACTIVE=dev`. При обычном запуске документация отключена.

1. В Swagger UI вызови `POST /api/auth/register` с новым логином или `POST /api/auth/login` для существующего аккаунта. Нажми **Authorize** и вставь значение `accessToken` без префикса `Bearer`.
2. Возьми ID своего пользователя из поля `userId` в полезной нагрузке access-токена, декодировав её локально. Создай тренировку через `POST /api/workout`, передав `userId`, `name` и `date` в формате `YYYY-MM-DD`. Запомни `id` из ответа.
3. Возьми `id` упражнения из каталога (`GET /api/exercise`). Добавь его в тренировку через `POST /api/workout-exercise`, передав `workoutId`, `exerciseId` и `orderIndex: 1`. ID из ответа — это `workoutExerciseId`.
4. Дважды вызови `POST /api/workout-set` с одинаковым телом. Подставь свой `workoutExerciseId` и новый UUID в `requestId`.

   ```json
   {
     "workoutExerciseId": 42,
     "position": 1,
     "repsDone": 10,
     "weight": 60.25,
     "requestId": "5a70817a-1f14-4c30-83dc-cff270165cdd"
   }
   ```

   Оба запроса возвращают `200` и один и тот же `id` подхода. В `GET /api/workout-set/workout-exercise/{workoutExerciseId}` будет один подход. Если повторить запрос с тем же `requestId`, но изменить `repsDone`, API вернёт `409 Conflict`.

### Авторизация

| Метод | Маршрут | Назначение |
| --- | --- | --- |
| `POST` | `/api/auth/register` | Создать аккаунт и войти |
| `POST` | `/api/auth/login` | Войти |
| `POST` | `/api/auth/refresh` | Ротировать refresh-токен и получить новую пару токенов |
| `POST` | `/api/auth/logout` | Отозвать refresh-токен |

Регистрация принимает логин, email и пароль:

```json
{
  "username": "alex",
  "email": "alex@example.com",
  "password": "password123"
}
```

Успешная регистрация и вход возвращают `accessToken` и `refreshToken`.

### Пользователи и профиль

| Метод | Маршрут | Доступ | Назначение |
| --- | --- | --- | --- |
| `GET` | `/api/user/{id}` | Владелец или администратор | Получить профиль |
| `POST` | `/api/user` | Администратор | Создать пользователя с ролью USER |
| `PATCH` | `/api/user/me/password` | Авторизованный пользователь | Сменить пароль, указав текущий |
| `DELETE` | `/api/user/{id}` | Владелец или администратор | Удалить аккаунт |

### Каталог упражнений

| Метод | Маршрут | Доступ | Назначение |
| --- | --- | --- | --- |
| `GET` | `/api/exercise` | Авторизованный пользователь | Получить страницу каталога |
| `GET` | `/api/exercise/{id}` | Авторизованный пользователь | Получить упражнение |
| `GET` | `/api/exercise/search?name=...` | Авторизованный пользователь | Найти упражнения по названию |
| `GET` | `/api/exercise/muscle-group/{muscleGroup}` | Авторизованный пользователь | Фильтровать по группе мышц |
| `POST` | `/api/exercise` | Администратор | Добавить упражнение |
| `PUT` | `/api/exercise/{id}` | Администратор | Изменить упражнение |
| `DELETE` | `/api/exercise/{id}` | Администратор | Удалить упражнение |

Для постраничных запросов каталога используются параметры `pageNumber` (начиная с 0) и `pageSize` (от 1 до 100).

### Тренировки и подходы

| Метод | Маршрут | Назначение |
| --- | --- | --- |
| `GET` | `/api/workout/{id}` | Получить тренировку |
| `GET` | `/api/workout/user/{userId}` | Получить тренировки пользователя; поддерживает `pageNumber` и `pageSize` |
| `POST` | `/api/workout` | Создать тренировку |
| `PUT` | `/api/workout/{id}` | Изменить тренировку |
| `DELETE` | `/api/workout/{id}` | Удалить тренировку |
| `GET` | `/api/workout-exercise/{id}` | Получить упражнение из тренировки |
| `GET` | `/api/workout-exercise/workout/{workoutId}` | Получить упражнения тренировки |
| `POST` | `/api/workout-exercise` | Добавить упражнение в тренировку |
| `PUT` | `/api/workout-exercise/{id}` | Изменить порядок упражнений |
| `DELETE` | `/api/workout-exercise/{id}` | Убрать упражнение из тренировки |
| `GET` | `/api/workout-set/{id}` | Получить подход |
| `GET` | `/api/workout-set/workout-exercise/{workoutExerciseId}` | Получить подходы упражнения |
| `POST` | `/api/workout-set` | Добавить подход |
| `PUT` | `/api/workout-set/{id}` | Изменить подход |
| `DELETE` | `/api/workout-set/{id}` | Удалить подход |

### Прогресс

| Метод | Маршрут | Назначение |
| --- | --- | --- |
| `GET` | `/api/progress?userId={id}&exerciseId={id}` | Получить историю подходов пользователя по упражнению |

## Данные и миграции

Flyway запускает SQL-файлы из `src/main/resources/db/migration` по порядку версий. Начальный каталог упражнений и связи удаления данных заданы в миграции `V3__cascade_and_seed.sql`.

Не изменяй уже применённые миграции: для последующих изменений схемы или стартовых данных добавляй новую миграцию с очередной версией.

Основные связи: тренировка принадлежит пользователю; упражнения и подходы принадлежат тренировке. При удалении пользователя удаляются его тренировки, а вместе с ними упражнения тренировок и подходы. Записи общего каталога упражнений удаляются отдельно.

## Авторизация и безопасность

- Access JWT действует 15 минут; refresh-токен — 7 дней.
- При обновлении refresh-токен одноразово ротируется. В базе хранится только его SHA-256 хэш.
- Пароли хранятся с BCrypt, а не в открытом виде.
- Длина пароля: минимум 8 символов и не более 72 байт в UTF-8.
- Redis ограничивает попытки входа, регистрации и неудачной смены пароля. Лимиты задаются в `src/main/resources/application.yaml`.
- Если Redis недоступен, приложение продолжает обслуживать запросы без применения rate limits.

## Тесты и сборка

Backend-тесты:

```bash
./mvnw test
```

Интеграционные тесты используют Testcontainers; для них должен быть запущен Docker.

Frontend-тесты и production-сборка:

```bash
cd frontend
npm test
npm run build
```

`npm run build` проверяет TypeScript и собирает frontend в `frontend/dist`.
