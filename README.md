# Система за консултации (MVP)

Уеб приложение за записване на консултации между студенти и преподаватели.
Оригиналното задание е в [PROMPT.md](PROMPT.md).

## Технологии

- Backend: Java 21, Spring Boot 3.3, Spring Security, Spring Data JPA, H2 (in-memory), Maven.
- Frontend: React 18, Vite, plain CSS, react-router-dom 6.
- Auth: server-side session cookie, BCrypt hashing.

## Структура

```
db-ai-proekt/
  backend/   Spring Boot REST API
  frontend/  React + Vite SPA
  PROMPT.md  оригинално задание
```

## Стартиране

### Backend

```bash
cd backend
mvn spring-boot:run
```

Сървърът стартира на `http://localhost:8080`.
H2 конзола: `http://localhost:8080/h2` (JDBC URL: `jdbc:h2:mem:consult`, user `sa`, празна парола).

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Приложението стартира на `http://localhost:5173` и прокси-ва `/api/*` към backend-а.

## Демо акаунти

Всички с парола `password`:

| Роля    | Email                 |
|---------|-----------------------|
| ADMIN   | admin@consult.bg      |
| TEACHER | teacher1@consult.bg   |
| TEACHER | teacher2@consult.bg   |
| STUDENT | student1@consult.bg   |
| STUDENT | student2@consult.bg   |
| STUDENT | student3@consult.bg   |

## Роли

- STUDENT: разглежда преподаватели и свободни слотове, записва се, отменя свое записване, вижда предстоящи и минали записвания.
- TEACHER: създава и изтрива собствени слотове (само свободни), вижда кой студент е записан за зает слот.
- ADMIN: read-only преглед на потребители и записвания.

---

# Системен дизайн

## 1. Високо ниво

Monorepo с две независими части: React SPA на Vite dev сървър и Spring Boot REST API. Комуникацията е JSON през HTTP; сесията се носи от HttpOnly cookie, което браузърът прикача автоматично.

```
    +----------------------+       HTTP/JSON       +---------------------------+
    |   React SPA (5173)   | <-------------------> |  Spring Boot API (8080)   |
    |  Vite dev server     |   cookie:             |  + Spring Security        |
    |  react-router-dom    |   CONSULT_SESSION     |  + Spring Data JPA        |
    |  plain CSS           |                       |  + BCrypt                 |
    +----------------------+                       +-------------+-------------+
                                                                 |
                                                                 v
                                                   +---------------------------+
                                                   |      H2 in-memory DB      |
                                                   |      (create-drop)        |
                                                   +---------------------------+
```

В dev Vite прокси-ва `/api/*` към `:8080`, така че от гледна точка на браузъра всичко е same-origin и cookie-то се изпраща без CORS complications.

## 2. Пакетна структура (backend)

```
com.consult
├── ConsultApplication               Spring Boot entry point
├── config
│   └── DataSeeder                   CommandLineRunner: seed users + демо слотове
├── model                            JPA entities + enums
│   ├── User, Role
│   ├── ConsultationSlot, SlotStatus
│   └── Booking, BookingStatus
├── repo                             Spring Data JPA репозитории
│   ├── UserRepository
│   ├── ConsultationSlotRepository   findByIdForUpdate, overlap query, search
│   └── BookingRepository            fetch-join заявки срещу N+1
├── security
│   ├── SecurityConfig               filter chain, CORS, session repo, auth manager
│   ├── AppUserDetails               адаптер User -> UserDetails
│   └── AppUserDetailsService        loadUserByUsername
├── dto                              records за request/response
│   ├── LoginRequest, MeResponse
│   ├── TeacherDto, UserDto
│   ├── SlotDto, CreateSlotRequest
│   ├── BookingDto, CreateBookingRequest
│   └── ErrorResponse
├── service
│   ├── SlotService                  overlap check, delete rule
│   └── BookingService               pessimistic lock, cancel flow
├── controller                       5 REST контролера (thin, делегират към service)
│   ├── AuthController
│   ├── TeacherController
│   ├── SlotController
│   ├── BookingController
│   └── AdminController
└── exception
    ├── NotFound/Conflict/Forbidden/BadRequestException
    └── GlobalExceptionHandler       @RestControllerAdvice, единен JSON
```

Controller-и са тънки: само парсват DTO, делегират към service, мапват към response DTO. Service съдържа бизнес логиката и транзакционните граници. Repo е чист persistence слой.

## 3. Модел на данните

```
   users                            consultation_slots                    bookings
   +----+-----------+---------+     +----+------------+----------+        +----+---------+------------+
   | id | full_name | email   |     | id | teacher_id | start_at |        | id | slot_id | student_id |
   |    | password  | role    |     |    | end_at     | location |        |    | status  | created_at |
   +----+-----------+---------+     |    | status     | version  |        +----+---------+------------+
         ^                          +-----------------+----------+            |         |
         |                                      ^                             |         |
         |  (teacher_id FK)                     |  (slot_id FK)               |         |
         +---------------------------------------+----------------------------+         |
         |                                                                              |
         |  (student_id FK)                                                             |
         +------------------------------------------------------------------------------+
```

Кардиналности:

- `User(TEACHER) 1..N ConsultationSlot`
- `User(STUDENT) 1..N Booking`
- `ConsultationSlot 1..N Booking` (като row-и), но най-много **една ACTIVE** в даден момент.

Жизнен цикъл на слот:

```
              create                   book                cancel
    (none) ---------> AVAILABLE ---------------> BOOKED -----------> AVAILABLE
                         |                          ^
                         | delete                   |
                         v                          |
                      (removed)              NOT allowed (409)
```

Жизнен цикъл на booking:

```
              POST /api/bookings                 DELETE /api/bookings/{id}
    (none) ----------------------> ACTIVE -------------------------------> CANCELLED
                                     ^                                         |
                                     |  (нов booking row)                      |
                                     +-----------------------------------------+
```

Когато студент отмени, booking-ът не се изтрива: `status` става `CANCELLED` (история), а slot-ът се връща на `AVAILABLE`. Друг студент може да резервира отново, създавайки **нов booking row** за същия slot. Така slot-ът може да има много CANCELLED booking-и и максимум една ACTIVE.

## 4. Поток: логин и session

```
  Browser                 SecurityFilter         AuthController      AuthenticationManager   UserDetailsService
     |                          |                      |                      |                      |
     | POST /api/auth/login     |                      |                      |                      |
     | {email, password}        |                      |                      |                      |
     |------------------------->| permitAll            |                      |                      |
     |                          |--------------------->|                      |                      |
     |                          |                      | authenticate(token)  |                      |
     |                          |                      |--------------------->|                      |
     |                          |                      |                      | loadByUsername       |
     |                          |                      |                      |--------------------->|
     |                          |                      |                      |  User row            |
     |                          |                      |                      |<---------------------|
     |                          |                      |  BCrypt verify       |                      |
     |                          |                      |  AppUserDetails      |                      |
     |                          |                      |<---------------------|                      |
     |                          |                      | context.save(req,res)|                      |
     |                          |                      | -> HttpSession       |                      |
     | Set-Cookie:              |                      |                      |                      |
     | CONSULT_SESSION=...      |                      |                      |                      |
     |<------------------------------------------------|                      |                      |
     | 200 {id, fullName, role} |                      |                      |                      |
     |<-------------------------|                      |                      |                      |
```

На следващи заявки `SecurityContextHolderFilter` чете SecurityContext от сесията по cookie-то; `@AuthenticationPrincipal AppUserDetails` е наличен в контролерите без допълнителен код.

## 5. Поток: записване (race-safe booking)

Критичният раздел е "слотът да стане BOOKED най-много веднъж". Имплементирано с **pessimistic lock** върху slot row-а плюс `@Version` на ConsultationSlot (belt-and-suspenders).

```
  Student A                  BookingService                        DB
     |                             |                                |
     | POST /api/bookings {42}     |                                |
     |---------------------------->|                                |
     |                             | @Transactional begin           |
     |                             | SELECT ... WHERE id=42 FOR UPD |
     |                             |------------------------------->|  row lock acquired
     |                             | slot                           |
     |                             |<-------------------------------|
     |                             | assert status == AVAILABLE     |
     |                             | slot.status = BOOKED           |
     |                             | INSERT booking(ACTIVE)         |
     |                             | commit                         |
     |                             |------------------------------->|  row lock released
     | 201 BookingDto              |                                |
     |<----------------------------|                                |

  Student B (конкурентно за slot 42)
     |                             |
     | POST /api/bookings {42}     |
     |---------------------------->|
     |                             | SELECT ... FOR UPDATE
     |                             | (блокира докато A commit-не)
     |                             | след unblock вижда status=BOOKED
     |                             | throw ConflictException
     | 409 "Слотът вече не е свободен."
     |<----------------------------|
```

`@Version` защитава допълнително от случайни stale writes ако slot-ът бъде променян и извън BookingService (напр. cancel от друга транзакция).

## 6. Поток: отказ от записване

```
  Student                  BookingService
     |                           |
     | DELETE /api/bookings/{id} |
     |-------------------------->|
     |                           | load booking
     |                           | if student != owner        -> 403
     |                           | if status != ACTIVE        -> 409
     |                           | booking.status = CANCELLED
     |                           | slot.status    = AVAILABLE
     |                           | commit (JPA dirty checking)
     | 204 No Content            |
     |<--------------------------|
```

## 7. Security модел

- **Session cookie**: `CONSULT_SESSION`, HttpOnly, SameSite=Lax, 60 мин idle timeout.
- **CSRF**: изключен. SameSite=Lax плюс same-origin SPA чрез Vite proxy елиминират реалистичния CSRF vector. Това е осъзната simplification за MVP; за production би се включил CSRF token exchange.
- **CORS**: разрешен origin `http://localhost:5173` с `allowCredentials=true`, за случаите когато frontend-ът говори директно с `:8080` без proxy.
- **Authentication**: `DaoAuthenticationProvider` + `AppUserDetailsService` + `BCryptPasswordEncoder`.
- **Authorization**:
  - Декларативно на метод ниво: `@PreAuthorize("hasRole('TEACHER')")`, `@PreAuthorize("hasRole('STUDENT')")`, клас-level `@PreAuthorize("hasRole('ADMIN')")` върху AdminController.
  - Owner-level проверки в service (студент не може да отменя чуждо записване, teacher не може да изтрива чужди слотове).
- **Разделение 401 / 403**:
  - `401 Unauthorized`: заявка без валидна сесия.
  - `403 Forbidden`: автентикиран, но ролята или owner check не позволяват.

## 8. Обработка на грешки

`GlobalExceptionHandler` издава единен JSON независимо от източника:

```
NotFoundException            -> 404
ConflictException            -> 409
ForbiddenException           -> 403
BadRequestException          -> 400
MethodArgumentNotValidException -> 400 (агрегирано съобщение от field errors)
AccessDeniedException        -> 403
AuthenticationException      -> 401
всяко друго                  -> 500
```

Примерен response:

```json
{
  "timestamp": "2026-04-17T10:23:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Слотът вече не е свободен.",
  "path": "/api/bookings"
}
```

## 9. Валидация

Валидацията е **двуслойна**:

- **DTO слой** (Jakarta Bean Validation върху records): `@NotBlank`, `@Email`, `@NotNull`. Задейства се от `@Valid` на контролера.
- **Domain слой** (в service):
  - `endAt > startAt` (BadRequest).
  - overlap за слотове на същия teacher (Conflict) - изчислено през query `startAt < :end AND endAt > :start`.
  - `slot.status == AVAILABLE` при booking (Conflict).
  - owner check при cancel (Forbidden).
  - delete слот само ако `status == AVAILABLE` (Conflict иначе).

## 10. Permissions matrix

| Endpoint                              | STUDENT    | TEACHER       | ADMIN | Anon |
|---------------------------------------|------------|---------------|-------|------|
| POST   /api/auth/login                | public     | public        | public| Y    |
| POST   /api/auth/logout               | Y          | Y             | Y     | Y    |
| GET    /api/auth/me                   | Y          | Y             | Y     | Y*   |
| GET    /api/teachers                  | Y          | Y             | Y     | N    |
| GET    /api/slots                     | Y (avail.) | Y (own, all)  | Y     | N    |
| POST   /api/slots                     | N          | Y             | N     | N    |
| DELETE /api/slots/{id}                | N          | Y (own, free) | N     | N    |
| GET    /api/bookings/me               | Y          | N             | N     | N    |
| POST   /api/bookings                  | Y          | N             | N     | N    |
| DELETE /api/bookings/{id}             | Y (own)    | N             | N     | N    |
| GET    /api/admin/users               | N          | N             | Y     | N    |
| GET    /api/admin/bookings            | N          | N             | Y     | N    |

`*` `GET /api/auth/me` без сесия връща 401, не грешка.

`GET /api/slots` поведение по роля:
- `STUDENT`, `ADMIN`: връща слотове със статус `AVAILABLE`, филтрирани по опционални `teacherId`, `from`, `to`.
- `TEACHER`: игнорира `teacherId`, връща **собствените** слотове (всички статуси), обогатени с `bookedByName`.

## 11. Frontend архитектура

```
main.jsx
  └── BrowserRouter
      └── AuthProvider            useAuth() -> { user, login, logout, loading }
          └── App (routes)
              ├── /login          Login
              ├── /               Home (redirect по role)
              ├── /student        ProtectedRoute(STUDENT) -> StudentDashboard
              ├── /teacher        ProtectedRoute(TEACHER) -> TeacherDashboard
              └── /admin          ProtectedRoute(ADMIN)   -> AdminDashboard
```

Ключови модули:

- `api.js`: тънък fetch wrapper с `credentials: 'include'`, единна обработка на JSON errors (вдига `Error` с `status` и `payload`).
- `AuthContext`: single source of truth за текущия user. При mount извиква `GET /api/auth/me`; 401 -> user остава null и `ProtectedRoute` пренасочва към `/login`.
- `ProtectedRoute`: проверява loading -> user -> role и съответно render/redirect.
- Три dashboard компонента, по един на роля, всеки пази собствен local state с `useState` + `useEffect` за първоначално зареждане.

## 12. Съзнателни tradeoffs

| Избор | Алтернатива | Защо този | Цена |
|-------|-------------|-----------|------|
| Session cookie | JWT | по-прост flow, без token refresh, съответства на спецификацията | хоризонтално мащабиране изисква sticky sessions или споделен session store |
| H2 in-memory | PostgreSQL/MySQL | нулев setup, бърза демонстрация | данните се губят при рестарт |
| Pessimistic lock на slot | unique partial index (WHERE status=ACTIVE) | четимо в кода, портируемо, кратки транзакции | минимална латентност при висока конкуренция на един slot |
| Overlap check в service | DB range constraint | по-просто за H2, по-лесно за обяснение | не е напълно атомично за един teacher |
| `open-in-view=false` | default true | избягва lazy loading в сериализация и N+1 | изисква експлицитни fetch joins |
| CSRF disabled | CSRF token | SPA е same-origin в dev чрез proxy, SameSite=Lax покрива повечето случаи | production deploy би добавил CSRF token exchange |
| plain CSS | CSS-in-JS / Tailwind | нулев build overhead, съответства на заданието | по-малко ergonomics |

## 13. Session lifecycle

- **Login**: създава `HttpSession`, `HttpSessionSecurityContextRepository` записва `SecurityContext` в нея, cookie се връща в response.
- **Всяка заявка**: `SecurityContextHolderFilter` вади контекста от сесията по cookie-то.
- **Logout**: `session.invalidate()` + `SecurityContextHolder.clearContext()`; cookie остава в браузъра но не съответства на валидна сесия.
- **Timeout**: 60 мин idle. След това следваща заявка връща 401 и frontend-ът води към `/login`.

---

## Seeded данни

`DataSeeder` (CommandLineRunner) зарежда при първи старт:

- 1 ADMIN: `admin@consult.bg`
- 2 TEACHER: `teacher1@consult.bg`, `teacher2@consult.bg`
- 3 STUDENT: `student1@consult.bg`, `student2@consult.bg`, `student3@consult.bg`
- 5 демо слота разпределени през следващите 3 дни

Парола за всички: `password` (BCrypt hash в базата).
