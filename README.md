# Fitness Tracker

Веб-приложение для силовых тренировок. В нём можно составить тренировку из каталога упражнений, записывать вес и повторы, следить за историей и повторять удачные тренировки. Интерфейс рассчитан на телефон и поддерживает установку как PWA.

- **Во время тренировки** доступны изменение порядка упражнений и таймер отдыха.
- **При потере связи** операции с подходами сохраняются на устройстве и отправляются после восстановления соединения.
- **Для анализа прогресса** доступна история подходов по упражнению и сравнение результатов с предыдущими тренировками.

## Быстрый запуск

Нужны Docker с Compose и OpenSSL для генерации локальных ключей. Выполняйте команды из корня репозитория в Bash. Если `.env` уже существует, используйте его и проверьте значения по примеру ниже.

```bash
if [ ! -f .env ]; then
  (
    umask 077
    cat > .env <<ENV
DB_URL=jdbc:postgresql://db:5432/fitness_db
DB_USERNAME=fitness
DB_PASSWORD=$(openssl rand -hex 16)
JWT_SECRET=$(openssl rand -hex 32)
ENV
  )
fi

docker compose up --build -d
```

Откройте [http://localhost:8080](http://localhost:8080), зарегистрируйтесь и создайте первую тренировку. Стартовый каталог упражнений заполняется миграцией Flyway. Состояние контейнеров можно проверить командой `docker compose ps`.

Для остановки используйте `docker compose down`. 

## Локальная разработка

Нужны JDK 21, Maven, Node.js 22.12+ из ветки 22.x и Docker Compose. Создайте `.env` по примеру выше. Если весь стек уже запущен через Compose, остановите контейнер приложения, чтобы освободить порт 8080.

```bash
docker compose stop app
docker compose up -d --wait db redis
```

Из корня репозитория запустите Spring Boot. Этот пример использует `.env`, созданный командой выше.

```bash
(
  set -a
  . ./.env
  set +a
  export DB_URL=jdbc:postgresql://localhost:5433/fitness_db
  export REDIS_HOST=localhost
  export REDIS_PORT=6379
  mvn spring-boot:run -Dspring-boot.run.profiles=dev
)
```

В другом терминале запустите React.

```bash
cd frontend
npm ci
npm run dev
```

Откройте [http://localhost:5173](http://localhost:5173). Vite проксирует запросы `/api/**` в Spring Boot на порту 8080. В Docker собранный фронтенд раздаёт сам Spring с того же адреса, что и REST API.

| Параметр | Назначение |
| --- | --- |
| `DB_URL` | JDBC-адрес PostgreSQL. В Compose используется `db:5432`, при запуске Spring на хосте  `localhost:5433`. |
| `DB_USERNAME`, `DB_PASSWORD` | Учётные данные PostgreSQL. |
| `JWT_SECRET` | Ключ подписи JWT. Для HS256 требуется не менее 32 байт. |
| `REDIS_HOST`, `REDIS_PORT` | Адрес Redis. В Compose приложение использует `redis:6379`, при запуске на хосте  `localhost:6379`. |

## REST API

В профиле `dev` доступны [Swagger UI](http://localhost:8080/swagger-ui.html) и [спецификация OpenAPI](http://localhost:8080/v3/api-docs). При обычном запуске документация отключена.

| Группа | Примеры маршрутов |
| --- | --- |
| Авторизация | `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/refresh`, `POST /api/auth/logout` |
| Профиль | `GET /api/user/{id}`, `PATCH /api/user/me/password` |
| Каталог | `GET /api/exercise`, `GET /api/exercise/search` |
| Тренировки | `POST /api/workout`, `GET /api/workout/{id}`, `GET /api/workout/user/{userId}` |
| Упражнения в тренировке | `POST /api/workout-exercise`, `GET /api/workout-exercise/workout/{workoutId}` |
| Подходы и прогресс | `POST /api/workout-set`, `GET /api/workout-set/workout-exercise/{workoutExerciseId}`, `GET /api/progress?userId=…&exerciseId=…` |

Защищённые маршруты `/api/**` требуют заголовок `Authorization: Bearer <accessToken>`. Маршруты `/api/auth/**` доступны без access-токена. Доступ к тренировкам ограничен владельцем и администратором. Изменение общего каталога упражнений требует роли `ADMIN`.

### Проверка повторной отправки подхода

1. В Swagger UI вызовите `POST /api/auth/register` или `POST /api/auth/login`. В **Authorize** вставьте `accessToken` без префикса `Bearer`.
2. Возьмите `userId` из полезной нагрузки JWT, декодировав её локально. Вызовите `POST /api/workout` с полями `userId`, `name` и `date` в формате `YYYY-MM-DD`.
3. Выберите упражнение через `GET /api/exercise`. Добавьте его через `POST /api/workout-exercise`, передав `workoutId`, `exerciseId` и `orderIndex: 1`. Из ответа возьмите `id` для поля `workoutExerciseId`.
4. Дважды вызовите `POST /api/workout-set` с одинаковым телом. Подставьте полученный `workoutExerciseId` и новый UUID в `requestId`.

   ```json
   {
     "workoutExerciseId": 42,
     "position": 1,
     "repsDone": 10,
     "weight": 60.25,
     "requestId": "5a70817a-1f14-4c30-83dc-cff270165cdd"
   }
   ```

   Оба запроса вернут `200 OK` с одним и тем же ID подхода. Запрос списка подходов покажет одну запись. Повтор с тем же `requestId` и изменённым `repsDone` вернёт `409 Conflict`.

## Архитектура

- [Бэкенд](src/main/java/com/Author/Fitness): Java 21, Spring Boot, Spring MVC, Spring Security и Spring Data JPA. Код сгруппирован по предметным областям. Контроллеры принимают запросы, сервисы проверяют доступ и выполняют операции, репозитории работают с данными, а MapStruct преобразует сущности и DTO.
- [Фронтенд](frontend/src): React, TypeScript, Vite, Tailwind CSS и TanStack Query. 
- [Данные и миграции](src/main/resources/db/migration): PostgreSQL хранит тренировки, подходы, хеши refresh-токенов и результаты запросов на создание подходов. Flyway применяет миграции при старте. Redis используется для счётчиков ограничения попыток входа, регистрации и смены пароля.
- [Сборка контейнера](Dockerfile): отдельные этапы собирают React и Spring Boot. Статика фронтенда включается в JAR. Spring отдаёт `index.html` для клиентских маршрутов, поэтому прямые переходы на страницы тренировок работают и после обновления страницы.

## Тесты и CI

Бэкенд. Запускайте из корня репозитория с доступным Docker.

```bash
mvn -B verify
```

Интеграционные тесты поднимают собственные PostgreSQL и Redis через Testcontainers. Уже запущенный стек Compose и его `.env` для тестов не нужны. Проверки охватывают авторизацию, права доступа и повторное создание подходов.

Фронтенд. Из корня репозитория выполните следующее.

```bash
cd frontend
npm ci
npm test
npm run build
```

Vitest проверяет, в частности, синхронизацию офлайн-очереди. Команда `npm run build` отдельно проверяет типы и собирает PWA.

[GitHub Actions](.github/workflows/ci.yml) запускает бэкенд и фронтенд в двух заданиях при push в `main` или `fresh-main`, а также при pull request в `main`. Workflow можно запустить вручную через вкладку **Actions**.
