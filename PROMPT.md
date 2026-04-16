# Оригинално задание

```
# Проект
Необходима е уеб система за консултации между студенти и преподаватели. Студентите трябва да могат да виждат свободни слотове и да се записват, преподавателите да управляват графика си, а администраторът да има централен read-only преглед на потребители и записвания.

Целта е бързо MVP решение с минимални, но завършени функции, което покрива основния процес по записване за консултация.

# Обхват на MVP
- Приложението има 3 роли: STUDENT, TEACHER, ADMIN.
- Достъпът е чрез login със seed потребители, без регистрация.
- Един слот може да бъде резервиран само от един студент.
- Админ панелът е read-only.

# Функционални изисквания по роли

## Student
- Влиза в системата.
- Вижда списък с преподаватели.
- Вижда свободни слотове по преподавател и дата.
- Записва се за свободен слот.
- Отменя собствено записване.
- Вижда списък със своите записвания: предстоящи и минали.

## Teacher
- Влиза в системата.
- Създава слотове за консултация с дата, начален/краен час и място.
- Вижда собствените си слотове.
- Изтрива собствен слот само ако не е резервиран.
- Вижда кой студент е записан за зает слот.

## Admin
- Влиза в системата.
- Вижда всички потребители read-only.
- Вижда всички записвания read-only.

# Нефункционални изисквания
- Бърз старт и лесна демонстрация за упражнения или курсова задача.
- Ясно role-based поведение и защита на API endpoints.
- Локално стартиране без външна база данни.

# Системен дизайн
- Архитектура: Monorepo
  - frontend/: React + Vite + plain CSS
  - backend/: Java Spring Boot (Maven)
- Тип приложение: REST API + SPA клиент.
- База данни: H2 in-memory, инициализирана при старт.
- Автентикация: session cookie, server-side session.

# UI/UX дизайн

## Login Page
- Полета: email, password.
- Валидация за задължителни полета.
- Грешка при невалидни данни за вход.

## Student Dashboard
- Секция „Преподаватели".
- Секция „Свободни слотове" с филтър по преподавател и дата.
- Бутон „Запиши" за свободен слот.
- Секция „Моите записвания" с бутон „Откажи".

## Teacher Dashboard
- Форма за създаване на слот.
- Таблица или лист със собствени слотове.
- Статус на слот: свободен / зает.
- Действие „Изтрий" само за свободни слотове.

## Admin Dashboard
- Таблица с всички потребители.
- Таблица с всички записвания.
- Само преглед, без бутони за промяна.

# UX принципи
- Прост и ясен интерфейс с минимални стъпки.
- Ясни съобщения за успех и грешка.
- Ролево пренасочване след login.
- Базова responsive поддръжка за desktop и mobile.

# Database objects

## User
- id (PK)
- full_name
- email (unique)
- password_hash
- role (STUDENT | TEACHER | ADMIN)

## ConsultationSlot
- id (PK)
- teacher_id (FK към User)
- start_at
- end_at
- location
- status (AVAILABLE | BOOKED | CANCELLED)

## Booking
- id (PK)
- slot_id (FK към ConsultationSlot, unique)
- student_id (FK към User)
- status (ACTIVE | CANCELLED)
- created_at

# Връзки
- Един преподавател има много слотове.
- Един студент има много записвания.
- Един слот има най-много едно активно записване.

# Имплементационни правила

## Технологии
- Frontend: React + Vite + plain CSS.
- Backend: Java 21 + Spring Boot 3.x.
- Build tool: Maven.
- Database: H2 in-memory.
- Password hashing: BCrypt.

## API
- POST /api/auth/login
- POST /api/auth/logout
- GET /api/auth/me
- GET /api/teachers
- GET /api/slots?teacherId=&from=&to=
- POST /api/slots: teacher only
- DELETE /api/slots/{id}: teacher only, ако слотът не е резервиран
- GET /api/bookings/me: student only
- POST /api/bookings: student only, body: slotId
- DELETE /api/bookings/{id}: student owner only
- GET /api/admin/users: admin only, read-only
- GET /api/admin/bookings: admin only, read-only

## Access rules
- Всеки endpoint е защитен според роля.
- Потребител вижда и променя само собствени данни, освен admin read-only справки.
- Неоторизирани заявки връщат 401.
- Неразрешени действия за роля връщат 403.

## Business rules
- Слот може да се резервира само ако е AVAILABLE.
- При успешна резервация слотът става BOOKED.
- Отказ от резервация освобождава слота обратно към AVAILABLE.
- Преподавател не може да изтрие зает слот.
- Студент не може да отменя чуждо записване.

# Validation rules

## Auth
- email е задължителен и валиден.
- password е задължителен.
- Невалиден login връща общо съобщение за грешка.

## ConsultationSlot
- teacherId трябва да е валиден преподавател.
- startAt и endAt са задължителни.
- endAt трябва да е строго след startAt.
- Забраняват се припокриващи се слотове за един и същ преподавател.
- location е задължително поле.

## Booking
- slotId е задължителен и трябва да съществува.
- Записване е позволено само за свободен слот.
- Втори опит за записване на същия слот връща 409.
- Отказ е позволен само за собствено активно записване.

# Error format
Единен JSON формат:
- timestamp
- status
- error
- message
- path

# Обобщение
- Frontend: React + Vite + plain CSS.
- Backend: Java Spring Boot.
- База: H2 in-memory.
- Роли: STUDENT, TEACHER, ADMIN.
- Auth: session cookie login със seed users.
- Booking правило: един студент на слот.
- Admin: read-only dashboard.

Не добавяй функционалности извън описаното.
```
