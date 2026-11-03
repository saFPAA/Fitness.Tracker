# Fitness Tracker

Приложение для ведения тренировок и просмотра прогресса. Backend предоставляет REST API, frontend - адаптивный PWA.

## Возможности
- Регистрация, вход, выход и обновление access/refresh токенов.
- Создание тренировок, добавление упражнений и подходов, просмотр истории.
- Общий каталог упражнений с поиском и фильтрацией по группе мышц.
- Просмотр прогресса пользователя по выбранному упражнению.
- Роли `USER` и `ADMIN` с проверкой доступа к данным пользователя.
- Смена пароля из профиля с проверкой текущего пароля.
- Ограничение частоты запросов входа, регистрации и смены пароля через Redis.
- PWA с офлайн-очередью изменений подходов и сохранением части данных на устройстве.

При регистрации пользователь вводит логин, почту и пароль. Вход выполняется по логину и паролю. Почта и логин в профиле не изменяются
. Подтверждения почты и восстановления пароля по email в проекте нет.

## Технологии

| Часть | Технологии                                                                            |
| --- |---------------------------------------------------------------------------------------|
| Backend | Java 21, Spring Boot 4, Spring MVC, Spring Data JPA, Spring Security, Bean Validation |
| База данных | PostgreSQL 17, Flyway                                                                 |
| Ограничение запросов | Redis                                                                                 |
| Авторизация | JWT, BCrypt, refresh-токены с хранением SHA-256 хэша                                  |
| Frontend | React 19, TypeScript, Vite, React Router, TanStack Query                              |
| Тесты | JUnit 5, Mockito, /....Spring Boot Test/, Testcontainers, /...Vitest/                 |
| Запуск | Maven, Docker, Docker Compose                                                         |

## Структура проекта

```text
src/main/java/com/Author/Fitness/
├── Auth/             Регистрация, вход, обновление и отзыв токенов
├── Security/         JWT, правила доступа, Redis лимиты
├── User/             Пользователи, профиль, смена пароля
├── Exercise/         Каталог упражнений
├── Workout/          Тренировки
├── WorkoutExercise/  Упражнения в тренировках
├── WorkoutSet/       Подходы
├── Tools/            Прогресс и служебные компоненты
└── Exception/        Обработка ошибок API

src/main/resources/
└── db/migration/     Миграции 

src/test/java/        Модульные и интеграционные тесты
frontend/src/         Страницы, компоненты, API-клиент и PWA
```

## Запуск всего приложения через Docker Compose

Нужны Docker Engine и Docker Compose v2. Создайте `.env` в корне проекта:

```dotenv
DB_USERNAME=fitness_db
DB_PASSWORD=Ваш_пароль
DB_URL=jdbc:postgresql://db:5432/fitness_db
JWT_SECRET=JWT_секрет
```

Сгенерировать секрет JWT можно командой:

```bash
openssl rand -base64 48
```

Запустить приложение:

```bash
docker compose up -d --build
```


## Локальная разработка

### Backend и инфраструктура

Создайте `.env`, как описано выше, затем запустите только PostgreSQL и Redis:

```bash
docker compose up -d db redis
```

Для запуска Spring Boot на хосте используйте адрес опубликованной базы `localhost:5433`. Загрузите переменные из `.env` и переопределите адреса для запуска вне Docker:

```bash
set -a
. ./.env
set +a
export DB_URL=jdbc:postgresql://localhost:5433/fitness_db
export REDIS_HOST=localhost
export REDIS_PORT=6379
./mvnw spring-boot:run
```

Flyway применит миграции при запуске. Локальный frontend использует Vite proxy `/api` на `http://localhost:8080`, поэтому backend должен работать на стандартном порту `8080`.

### Frontend

Нужен Node.js. Из каталога `frontend` установите зависимости и запустите Vite:

```bash
cd frontend
npm ci
npm run dev
```

## API

Все адреса `/api/**`, кроме `/api/auth/**`, требуют заголовок:

```http
Authorization: Bearer <accessToken>
```

| Метод | Адрес | Назначение |
| --- | --- | --- |
| `POST` | `/api/auth/register` | Регистрация по логину, почте и паролю |
| `POST` | `/api/auth/login` | Вход по логину и паролю |
| `POST` | `/api/auth/refresh` | Обновление пары токенов по refresh-токену |
| `POST` | `/api/auth/logout` | Отзыв refresh-токена |
| `GET` | `/api/user/{id}` | Получение профиля с проверкой прав |
| `PATCH` | `/api/user/me/password` | Смена пароля по текущему паролю |
| `DELETE` | `/api/user/{id}` | Удаление пользователя с проверкой прав |
| `GET` | `/api/exercise` | Каталог упражнений; параметры `pageNumber`, `pageSize` |
| `GET` | `/api/exercise/{id}` | Упражнение по ID |
| `GET` | `/api/exercise/search?name=...` | Поиск упражнений по названию |
| `GET` | `/api/exercise/muscle-group/{muscleGroup}` | Фильтр упражнений по группе мышц |
| `POST` | `/api/exercise` | Создание упражнения (администратор) |
| `PUT` | `/api/exercise/{id}` | Изменение упражнения (администратор) |
| `DELETE` | `/api/exercise/{id}` | Удаление упражнения (администратор) |
| `GET` | `/api/workout/{id}` | Получение тренировки |
| `GET` | `/api/workout/user/{userId}` | Список тренировок пользователя; `pageNumber`, `pageSize` |
| `POST` | `/api/workout` | Создание тренировки |
| `PUT` | `/api/workout/{id}` | Изменение тренировки |
| `DELETE` | `/api/workout/{id}` | Удаление тренировки |
| `GET` | `/api/workout-exercise/{id}` | Упражнение в тренировке по ID |
| `GET` | `/api/workout-exercise/workout/{workoutId}` | Упражнения тренировки |
| `POST` | `/api/workout-exercise` | Добавление упражнения в тренировку |
| `PUT` | `/api/workout-exercise/{id}` | Изменение порядка упражнения |
| `DELETE` | `/api/workout-exercise/{id}` | Удаление упражнения из тренировки |
| `GET` | `/api/workout-set/{id}` | Подход по ID |
| `GET` | `/api/workout-set/workout-exercise/{workoutExerciseId}` | Подходы упражнения в тренировке |
| `POST` | `/api/workout-set` | Добавление подхода |
| `PUT` | `/api/workout-set/{id}` | Изменение подхода |
| `DELETE` | `/api/workout-set/{id}` | Удаление подхода |
| `GET` | `/api/progress?userId=...&exerciseId=...` | История прогресса по упражнению |


## Авторизация и ограничения запросов

- Access JWT действует 15 минут.
- Refresh-токен действует 7 дней. В базе хранится его SHA-256 хэш.
- При обновлении старый refresh-токен отзывается и выдаётся новый.
- Повторное использование отозванного refresh-токена отзывает активные refresh-токены пользователя.
- Пароли хранятся с BCrypt. Минимальная длина — 8 символов; при кодировании учитывается лимит 72 UTF-8 байта.
- Redis ограничивает неудачные попытки входа по логину и IP, количество регистраций с IP и неудачные попытки смены пароля по пользователю и IP.
- Значения лимитов и интервалы задаются в `src/main/resources/application.yaml` в секции `security.login-rate-limit`.
- При недоступности Redis приложение продолжает обрабатывать запросы без применения лимитов.

Регистрация создаёт пользователя с ролью `USER`. Для изменения роли администратора используется база данных. После смены роли нужно войти снова, чтобы получить access-токен с актуальными полномочиями.

Например, назначить роль можно SQL запросом:

```sql
UPDATE users SET role = 'ADMIN' WHERE username = 'ваш_логин';
```

## Тесты и сборка

### Backend

```bash
./mvnw test
```

Интеграционные тесты используют PostgreSQL и Redis через Testcontainers. 

Собрать backend без запуска тестов:

```bash
./mvnw -DskipTests package
```

### Frontend

```bash
cd frontend
npm test
npm run build
```

`npm run build` проверяет TypeScript и собирает frontend в `frontend/dist`.

## Переменные окружения

| Переменная | Назначение |
| --- | --- |
| `DB_URL` | JDBC URL PostgreSQL |
| `DB_USERNAME` | имя пользователя базы данных |
| `DB_PASSWORD` | пароль базы данных |
| `JWT_SECRET` | секрет JWT|
| `PORT` | порт backend, по умолчанию `8080` |
| `REDIS_HOST` | адрес Redis, по умолчанию `localhost` |
| `REDIS_PORT` | порт Redis, по умолчанию `6379` |
