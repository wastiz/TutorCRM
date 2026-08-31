# CLAUDE.md

# Tutor Management System

## 1. Project Overview

Разработать небольшое веб-приложение для репетитора, которое заменяет ручное ведение учеников, уроков и ежемесячной отчётности.

Приложение должно стать основным источником информации о:

* учениках;
* расписании;
* проведённых уроках;
* стоимости уроков;
* ежемесячной статистике;
* сумме к выплате.

При этом приложение должно интегрироваться с уже существующими:

* Google Calendar;
* Google Sheets.

Google Calendar и Google Sheets **не принадлежат приложению**. Они принадлежат другим аккаунтам/организациям, но текущий пользователь может иметь к ним доступ.

Интеграции должны работать через Google OAuth от имени текущего пользователя.

---

# 2. Current Business Process

Сейчас процесс выглядит следующим образом:

1. Репетитору присылают ученика в Messenger.
2. Сообщение приходит в виде строки с полями, разделёнными TAB или пробелом.
3. Репетитор отвечает, когда может провести первый урок.
4. Репетитор вручную создаёт первый урок в Google Calendar.
5. Репетитор вручную создаёт последующие уроки.
6. В течение месяца уроки проходят.
7. В конце месяца репетитор вручную считает количество проведённых уроков.
8. Репетитор вручную заполняет Google Sheets.
9. На основании количества уроков рассчитывается сумма к выплате.

---

# 3. Desired Business Process

Новый процесс:

```text
Messenger
    ↓
Raw student message
    ↓
Tutor Management System
    ↓
Student Import Parser
    ↓
Preview
    ↓
Tutor confirms or corrects and confirms
    ↓
Student created
    ↓
Create first lesson
    ↓
Google Calendar
    ↓
Generate recurring lessons after first lesson
    ↓
Lessons are completed
    ↓
Monthly Report
    ↓
Google Sheets
```

---

# 4. Technology Stack

## Backend

* Java 21
* Spring Boot 3.x
* Spring Web
* Spring Security
* Spring Data JPA
* Spring Validation
* Spring OAuth2 Client
* Spring Scheduler
* Spring Actuator
* PostgreSQL
* Liquibase
* MapStruct
* Lombok

Build tool:

* Gradle

---

# 5. Frontend

Использовать:

* Angular
* TypeScript
* Angular Router
* Angular Signals
* RxJS
* Angular Material

Для календарного интерфейса можно использовать:

* FullCalendar

Frontend должен быть SPA.

---

# 6. Database

Использовать PostgreSQL.

Все изменения структуры БД должны выполняться через Liquibase migrations.

Не создавать схему БД вручную.

---

# 7. Deployment

Приложение должно деплоиться на:

## Railway

Предполагаемая инфраструктура:

```text
Railway
│
├── Backend
│
├── Frontend
│
└── PostgreSQL
```

Backend и Frontend могут быть отдельными Railway services.

PostgreSQL должен использоваться как Railway PostgreSQL service.

Не использовать локальные файлы для хранения production data.

Все секреты должны передаваться через environment variables.

Например:

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD

GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET

APP_BASE_URL

JWT_SECRET
```

Никакие secrets не должны попадать в Git.

---

# 8. Architecture

Использовать **Vertical Slice Architecture по сущностям**.

Не использовать классическую глобальную структуру:

```text
controller/
service/
repository/
model/
```

То есть НЕ делать:

```text
controller/
    StudentController
    LessonController

service/
    StudentService
    LessonService

repository/
    StudentRepository
    LessonRepository
```

Вместо этого каждая domain entity должна иметь собственный vertical slice.

---

# 9. Entity Structure

Основные entities:

* User
* Student
* Lesson
* Authentication
* Report

Структура:

```text
student/
    StudentController
    StudentService
    StudentRepository
    Student
    StudentDto
    StudentMapper

lesson/
    LessonController
    LessonService
    LessonRepository
    Lesson
    LessonDto
    LessonMapper

user/
    UserController
    UserService
    UserRepository
    User
    UserDto
    UserMapper

authentication/
    AuthenticationController
    AuthenticationService
    AuthenticationRepository
    Authentication
    AuthenticationDto

report/
    ReportController
    ReportService
    ReportRepository
    Report
    ReportDto
```

Каждая сущность должна содержать всё необходимое для работы с ней.

Общие infrastructure-компоненты допускаются отдельно:

```text
common/
config/
security/
integration/
```

Но domain-specific код должен оставаться внутри соответствующего vertical slice.

---

# 10. Main Entities

## 10.1 User

Пользователь приложения.

Поля:

```java
UUID id;

String email;

String firstName;

String lastName;

String googleSubject;

Instant createdAt;

Instant updatedAt;
```

Google Subject (`sub`) использовать как стабильный внешний идентификатор Google-пользователя.

---

# 10.2 Authentication

Отвечает за связь пользователя с Google OAuth.

Поля:

```java
UUID id;

UUID userId;

String provider;

String accessToken;

String refreshToken;

Instant accessTokenExpiresAt;

Instant createdAt;

Instant updatedAt;
```

Provider:

```text
GOOGLE
```

Access token и refresh token должны храниться безопасно.

В production желательно использовать encryption at rest для OAuth credentials.

---

# 10.3 Student

Поля:

```java
UUID id;

String firstName;

String lastName;

String email;

String phone;

String personal_code; (эстонский isikukood)

Integer age;

Integer grade;

String school;

String subject;

String goal;

String level;

String notes;

LessonFormat lessonFormat;

BigDecimal lessonPrice;

StudentStatus status;

LocalDate startDate;

LocalDate endDate;

String parentName;

String parentPhone;

String parentEmail;

Instant createdAt;

Instant updatedAt;
```

Enums:

```java
StudentStatus:
ACTIVE
PAUSED
FINISHED
```

```java
LessonFormat:
ONLINE
OFFLINE
BOTH
```

`lessonPrice` является текущей стоимостью урока.

---

# 10.4 Lesson

Каждый проведённый или запланированный урок должен быть отдельной сущностью.

Поля:

```java
UUID id;

UUID userId;

UUID studentId;

OffsetDateTime startTime;

OffsetDateTime endTime;

BigDecimal price;

LessonStatus status;

String notes;

String googleCalendarId;

String googleCalendarEventId;

Instant createdAt;

Instant updatedAt;
```

Enums:

```java
LessonStatus:
PLANNED
COMPLETED
CANCELLED
NO_SHOW
```

## Important

Цена должна сохраняться непосредственно в Lesson.

Нельзя при построении старого отчёта брать текущую цену Student.

Например:

```text
January:
Student price = €20

February:
Student price = €25
```

Старые January lessons должны остаться по €20.

---

# 10.5 Report

Смотреть Report.md

Поля:

```java
UUID id;

UUID userId;

YearMonth month;

Integer totalLessons;

BigDecimal totalAmount;

Instant generatedAt;
```

Report должен основываться на Lesson.

Для расчёта выплаты учитывать только:

```text
COMPLETED
```

Не учитывать:

```text
PLANNED
CANCELLED
NO_SHOW
```

---

# 11. Student Import

Это одна из ключевых функций приложения.

Пользователь должен иметь возможность создать ученика не только через обычную форму, но и через **Raw Message Import**.

---

# 12. Student Import UI

На странице создания ученика должны быть два варианта:

```text
Create Student

[ Manual ] [ Import from Message ]
```

Для `Import from Message` отображать textarea:

```text
Paste student message here...
```

Пользователь вставляет исходное сообщение, которое ему прислали в Messenger.

Например:

```text
Kirill Tsarenkov	ljulap@gmail.com	5350 6894	14	9	Tlvl	Эстонский язык	Слабый, особенно речь,нужна подготовка к экзамену 	ЧЕТВЕРГ	17-20	2	оба варианта подходят		Liudmila Lapshina 		51109180029	47808060232
```

---

# 13. Raw Message Format

Исходное сообщение представляет собой одну строку.

Поля разделены символом:

```text
TAB
```

То есть технически:

```text
String.split("\t")
```

может быть базовым механизмом разбора.

Но нельзя просто делать blind mapping без валидации.

Parser должен:

1. получить raw input;
2. нормализовать whitespace;
3. split по TAB;
4. определить количество полей;
5. удалить BOM, если он присутствует;
6. trim каждого значения;
7. сохранить пустые поля как `null`/empty;
8. выполнить validation;
9. вернуть структурированный DTO.

---

# 14. Import Field Mapping

На текущем этапе предполагается следующая структура:

```text
0  Full name
1  Student email
2  Student phone
3  Age
4  Grade
5  School
6  Subject
7  Goal / Description
8  Preferred days
9  Preferred time
10 Lessons per week
11 Lesson format
12 Unknown / Reserved field
13 Parent name
14 Parent phone
15 Parent secondary phone / reserved
```

Пример:

```text
Kirill Tsarenkov
ljulap@gmail.com
5350 6894
14
9
Tlvl
Эстонский язык
Слабый, особенно речь, нужна подготовка к экзамену
ЧЕТВЕРГ
17-20
2
оба варианта подходят
(empty)
Liudmila Lapshina
51109180029
47808060232
```

должен превращаться примерно в:

```json
{
  "firstName": "Kirill",
  "lastName": "Tsarenkov",
  "email": "ljulap@gmail.com",
  "phone": "5350 6894",
  "age": 14,
  "grade": 9,
  "school": "Tlvl",
  "subject": "Эстонский язык",
  "goal": "Слабый, особенно речь, нужна подготовка к экзамену",
  "preferredDays": ["THURSDAY"],
  "preferredTimeFrom": "17:00",
  "preferredTimeTo": "20:00",
  "lessonsPerWeek": 2,
  "lessonFormat": "BOTH",
  "parentName": "Liudmila Lapshina",
  "parentPhone": "51109180029",
  "parentSecondaryPhone": "47808060232"
}
```

---

# 15. Important Import Requirement

**Не сохранять ученика сразу после вставки raw message.**

Сначала показывать Preview.

Flow:

```text
Paste message
       ↓
Parse
       ↓
Validate
       ↓
Preview
       ↓
User confirms
       ↓
Create Student
```

---

# 16. Import Preview

После parsing frontend должен показать пользователю нормальную форму:

```text
Student Preview

Name:
Kirill Tsarenkov

Email:
ljulap@gmail.com

Phone:
5350 6894

Age:
14

Grade:
9

School:
Tlvl

Subject:
Эстонский язык

Goal:
Слабый, особенно речь, нужна подготовка к экзамену

Preferred days:
Thursday

Preferred time:
17:00 - 20:00

Lessons per week:
2

Format:
Online + Offline

Parent:
Liudmila Lapshina

Parent phone:
51109180029
```

Пользователь должен иметь возможность исправить данные перед сохранением.

Кнопки:

```text
[Cancel]
[Edit]
[Create Student]
```

---

# 17. Import Parser Architecture

Создать:

```text
student/
    import/
        StudentImportController
        StudentImportService
        StudentImportParser
        StudentImportDto
        StudentImportPreviewDto
```

Parser не должен заниматься сохранением в БД.

Parser отвечает только за:

```text
raw text
    ↓
structured DTO
```

Service отвечает за:

```text
DTO
    ↓
validation
    ↓
Student entity
    ↓
database
```

---

# 18. Parsing Rules

## Full Name

Первое поле содержит имя и фамилию.

Минимально:

```text
FirstName LastName
```

Разделить по первому whitespace.

Например:

```text
Kirill Tsarenkov
```

→

```text
firstName = Kirill
lastName = Tsarenkov
```

Если имя состоит из большего количества частей, parser не должен молча терять информацию.

Например:

```text
Anna Maria van Smith
```

В таком случае:

```text
firstName = Anna
lastName = Maria van Smith
```

---

# 19. Phone Numbers

Телефоны не должны преобразовываться в Integer.

Хранить как String.

Причина:

* `+372...`
* ведущие нули;
* пробелы;
* разные форматы;
* длинные номера.

---

# 20. Age and Grade

Парсер должен попытаться преобразовать:

```text
14
9
```

в:

```java
Integer
```

Если преобразование невозможно:

```text
age = null
grade = null
```

и показать warning в Preview.

Не падать с Internal Server Error.

---

# 21. Preferred Days

Поле может содержать:

```text
ЧЕТВЕРГ
```

или:

```text
ПЯТНИЦА, ВОСКРЕСЕНЬЕ
```

Необходимо создать mapping:

```text
ПОНЕДЕЛЬНИК -> MONDAY
ВТОРНИК -> TUESDAY
СРЕДА -> WEDNESDAY
ЧЕТВЕРГ -> THURSDAY
ПЯТНИЦА -> FRIDAY
СУББОТА -> SATURDAY
ВОСКРЕСЕНЬЕ -> SUNDAY
```

Также поддержать lowercase/mixed case.

Например:

```text
Четверг
четверг
ЧЕТВЕРГ
```

должны давать одинаковый результат.

---

# 22. Preferred Time

Вход может быть:

```text
17-20
```

Это означает:

```text
17:00 - 20:00
```

Возможны также значения:

```text
17:00-20:00
17 - 20
17:00 - 20:00
```

Parser должен нормализовать распространённые варианты.

Если формат неизвестен, сохранить raw value и показать warning.

---

# 23. Lesson Format Mapping

Поддержать:

```text
онлайн -> ONLINE
online -> ONLINE

оффлайн -> OFFLINE
offline -> OFFLINE

оба варианта подходят -> BOTH
оба -> BOTH
```

Parser должен быть case-insensitive.

---

# 24. Unknown Fields

Формат входящего сообщения может измениться.

Поэтому parser должен быть tolerant.

Если количество полей отличается от ожидаемого:

```text
expected: 16
actual: 17
```

не выбрасывать необработанное исключение.

Вместо этого вернуть:

```text
warnings:
- Unexpected number of fields: expected 16, got 17
```

и показать пользователю Preview.

Необходимо сохранить raw input в import DTO для debugging.

---

# 25. LLM Parsing

LLM НЕ использовать в первой версии.

Первый parser должен быть deterministic.

Причина:

* текущий формат структурированный;
* TAB является явным разделителем;
* deterministic parser проще тестировать;
* не нужны расходы на API;
* результат должен быть предсказуемым.

Архитектуру parser сделать расширяемой.

В будущем можно добавить:

```text
StudentImportParser
        ↑
        |
-----------------------
|                     |
TabStudentParser      AiStudentParser
```

---

# 26. Duplicate Detection

При создании ученика проверять возможные дубликаты.

Основные критерии:

1. email;
2. phone;
3. комбинация firstName + lastName.

Если найден похожий ученик:

```text
Possible duplicate found:

Kirill Tsarenkov
ljulap@gmail.com

[Use existing student]
[Create anyway]
```

---

# 27. Student Schedule

Для ученика необходимо хранить предпочитаемые дни/время.

Можно использовать отдельную сущность:

```text
student/
    schedule/
        StudentSchedule
        StudentScheduleRepository
```

Поля:

```java
UUID id;

UUID studentId;

DayOfWeek dayOfWeek;

LocalTime startTime;

LocalTime endTime;

Integer lessonsPerWeek;
```

Не хранить расписание одной строкой.

Например:

```text
"ПЯТНИЦА, ВОСКРЕСЕНЬЕ"
```

должно быть нормализовано в отдельные записи.

---

# 28. Google OAuth

Использовать Google OAuth 2.0.

Login flow:

```text
Frontend
    ↓
Backend
    ↓
Google OAuth
    ↓
Google consent
    ↓
Callback
    ↓
User
    ↓
Authentication
```

Не хранить Google password.

---

# 29. Google API Scopes

Использовать минимально необходимые scopes.

Не запрашивать полный Google Drive доступ без необходимости.

Необходимо получить доступ к:

* Google Calendar;
* Google Sheets.

Приложение должно работать с ресурсами, к которым у пользователя уже есть соответствующие права.

---

# 30. Google Calendar

Создать:

```text
integration/
    google/
        calendar/
            GoogleCalendarService
            GoogleCalendarClient
            GoogleCalendarMapper
```

Функциональность:

```java
createEvent()
updateEvent()
deleteEvent()
getCalendars()
```

---

# 31. Calendar Access

Важно:

Google Calendar и Google Sheets могут принадлежать не текущему пользователю.

Это нормально.

OAuth выполняется от имени текущего пользователя.

Например:

```text
Google Account
     |
     +---- Calendar owned by another person
     |         |
     |         +-- User has edit access
     |
     +---- Spreadsheet owned by another person
               |
               +-- User has edit access
```

API должен использовать права текущего пользователя.

Если пользователь не имеет прав на запись:

```text
403 / insufficient permissions
```

должен преобразовываться в понятное сообщение UI.

---

# 32. Calendar Event

При создании Lesson:

```text
Lesson
   ↓
Google Calendar Event
```

Сохранять:

```text
googleCalendarId
googleCalendarEventId
```

в Lesson.

---

# 33. Calendar Event Metadata

Использовать Google Calendar extended properties для хранения:

```text
application = tutor-management
lessonId = <UUID>
```

Это позволит в будущем связывать события Google Calendar с Lesson.

---

# 34. Lesson Creation

Flow:

```text
Create Lesson
      ↓
Validate
      ↓
Save Lesson
      ↓
Create Google Calendar Event
      ↓
Save calendarEventId
```

Если Google Calendar API недоступен, система не должна терять Lesson.

Необходимо определить transaction strategy.

Рекомендуемый MVP вариант:

```text
Lesson = PLANNED
googleCalendarEventId = null
syncStatus = FAILED
```

и дать пользователю возможность повторить синхронизацию.

---

# 35. Calendar Synchronization

Добавить:

```java
CalendarSyncStatus:

PENDING
SYNCED
FAILED
```

Lesson должен иметь:

```java
CalendarSyncStatus calendarSyncStatus;
```

Это поможет избежать потери данных.

---

# 36. Recurring Lessons

После создания первого урока пользователь должен иметь возможность создать последующие уроки.

Например:

```text
Every Thursday
17:00
60 minutes

Repeat:
4 weeks
```

Система создаёт несколько Lesson records.

Каждый Lesson имеет отдельный Google Calendar Event.

Не использовать один Lesson для серии уроков.

---

# 37. Lesson Completion

На странице урока пользователь должен иметь возможность:

```text
[Mark as Completed]
[Cancel]
[No Show]
```

При нажатии:

```text
Lesson.status = COMPLETED
```

Этот урок автоматически учитывается в Monthly Report.

---

# 38. Dashboard

Главная страница должна показывать:

```text
Active Students
Today's Lessons
This Week's Lessons
Current Month Lessons
Current Month Earnings
```

Пример:

```text
Active Students: 12

Today:
3 lessons

This month:
28 completed lessons

Expected earnings:
€720
```

---

# 39. Students Page

Таблица:

```text
Name
Subject
Grade
Format
Price
Status
Next Lesson
```

Функции:

```text
Search
Filter
Create
Edit
Archive
Open Student
```

---

# 40. Student Details

Показывать:

```text
Student information

Schedule

Upcoming lessons

Past lessons

Lessons this month

Total earnings

Parent information
```

---

# 41. Calendar Page

Показывать уроки приложения.

Использовать FullCalendar или Angular-compatible calendar library.

Функции:

* month view;
* week view;
* day view;
* create lesson;
* edit lesson;
* mark completed;
* cancel lesson.

---

# 42. Reports

Страница:

```text
Reports

Month:
[ August 2026 ]

Total lessons:
28

Total earnings:
€720
```

Разбивка:

```text
Student              Lessons    Price    Total

Kirill Tsarenkov       8        €25      €200
Artjom Zimin           10       €25      €250
Other Student          10       €27      €270

TOTAL                  28                 €720
```

---

# 43. Report Calculation

Для выбранного месяца:

```text
start = first day of month
end = first day of next month
```

Выбрать:

```text
Lesson.status = COMPLETED
```

и:

```text
startTime >= start
startTime < end
```

Total:

```text
SUM(lesson.price)
```

Количество:

```text
COUNT(lesson)
```

Не использовать текущую цену Student.

---

# 44. Google Sheets Export

На Reports Page:

```text
[Export to Google Sheets]
```

Flow:

```text
Generate Report
      ↓
Google Sheets API
      ↓
Create/update spreadsheet
      ↓
Write rows
```

---

# 45. Spreadsheet Output

Формировать таблицу:

```text
Student | Lessons | Price | Total
```

Например:

```text
Kirill Tsarenkov | 8  | 25 | 200
Artjom Zimin     | 10 | 25 | 250
Ivan Ivanov      | 10 | 27 | 270

TOTAL            | 28 |    | 720
```

---

# 46. Existing Google Sheet

Приложение должно поддерживать работу с существующей Google Sheet, к которой пользователь имеет доступ.

Не предполагать, что Spreadsheet принадлежит пользователю.

В настройках приложения пользователь должен иметь возможность выбрать:

```text
Google Spreadsheet
```

и:

```text
Sheet / Worksheet
```

которые используются для monthly reports.

---

# 47. Google Integration Settings

Создать страницу:

```text
Settings
```

Раздел:

```text
Google Integration

Google Account:
Connected

Calendar:
[Select Calendar]

Monthly Report Spreadsheet:
[Select Spreadsheet]

Worksheet:
[Select Worksheet]
```

Кнопки:

```text
[Reconnect Google]
[Disconnect Google]
```

---

# 48. REST API

## Student

```http
GET    /api/students
GET    /api/students/{id}
POST   /api/students
PUT    /api/students/{id}
DELETE /api/students/{id}
```

---

# 49. Student Import API

```http
POST /api/students/import/parse
```

Request:

```json
{
  "rawText": "Kirill Tsarenkov\tljulap@gmail.com\t..."
}
```

Response:

```json
{
  "student": {
    "firstName": "Kirill",
    "lastName": "Tsarenkov",
    "email": "ljulap@gmail.com"
  },
  "warnings": []
}
```

После Preview:

```http
POST /api/students/import/create
```

Request:

```json
{
  "firstName": "Kirill",
  "lastName": "Tsarenkov",
  ...
}
```

Этот endpoint создаёт Student.

---

# 50. Lesson API

```http
GET    /api/lessons
GET    /api/lessons/{id}
POST   /api/lessons
PUT    /api/lessons/{id}
DELETE /api/lessons/{id}

POST   /api/lessons/{id}/complete
POST   /api/lessons/{id}/cancel
POST   /api/lessons/{id}/no-show
POST   /api/lessons/{id}/sync-calendar
```

---

# 51. Report API

```http
GET /api/reports/monthly?month=2026-08
```

Response:

```json
{
  "month": "2026-08",
  "totalLessons": 28,
  "totalAmount": 720,
  "students": []
}
```

Export:

```http
POST /api/reports/monthly/export
```

---

# 52. Authentication API

```http
GET /oauth2/authorization/google
GET /login/oauth2/code/google
POST /api/auth/logout
GET /api/auth/me
```

---

# 53. Error Handling

Использовать centralized exception handling:

```java
@RestControllerAdvice
```

Ошибки должны возвращаться в формате:

```json
{
  "code": "STUDENT_NOT_FOUND",
  "message": "Student not found",
  "timestamp": "...",
  "path": "..."
}
```

Не возвращать stack traces пользователю.

---

# 54. Validation

Использовать Jakarta Bean Validation.

Проверять:

* email;
* age;
* grade;
* lesson price;
* lesson duration;
* required fields;
* date/time consistency.

---

# 55. Logging

Использовать SLF4J + Logback.

Логировать:

* authentication;
* Google OAuth;
* Calendar API operations;
* Sheets API operations;
* student imports;
* report generation.

Никогда не логировать:

* access tokens;
* refresh tokens;
* passwords;
* sensitive OAuth credentials.

---

# 56. Testing

Backend:

* JUnit 5
* Mockito
* Spring Boot Test
* Testcontainers PostgreSQL

Обязательно покрыть тестами:

## StudentImportParser

Минимум:

1. обычный валидный input;
2. пустые поля;
3. разные варианты регистра;
4. несколько дней недели;
5. разные форматы времени;
6. неправильный age;
7. неправильный grade;
8. неизвестный lesson format;
9. неправильное количество полей;
10. лишние whitespace.

Особенно тщательно тестировать реальные примеры входных данных.

---

# 57. Example Input #1

```text
Artjom Zimin	artemzimin171@gmail.com	58170531	18	12	TTG	Математика	Подготовка к экзаменам в 12 классе.Уровень слабый.	, ПЯТНИЦА, ВОСКРЕСЕНЬЕ		1	онлайн		Julia Zimina		50711217011	48303010225
```

Parser должен быть способен обработать:

* пустое поле;
* лишнюю запятую перед днями;
* отсутствие времени;
* `онлайн`;
* parent information.

---

# 58. Example Input #2

```text
Kirill Tsarenkov	ljulap@gmail.com	5350 6894	14	9	Tlvl	Эстонский язык	Слабый, особенно речь,нужна подготовка к экзамену 	ЧЕТВЕРГ	17-20	2	оба варианта подходят		Liudmila Lapshina 		51109180029	47808060232
```

Ожидаемый результат:

```text
First name:
Kirill

Last name:
Tsarenkov

Email:
ljulap@gmail.com

Phone:
5350 6894

Age:
14

Grade:
9

School:
Tlvl

Subject:
Эстонский язык

Goal:
Слабый, особенно речь, нужна подготовка к экзамену

Preferred day:
Thursday

Preferred time:
17:00 - 20:00

Lessons per week:
2

Format:
BOTH

Parent:
Liudmila Lapshina

Parent phone:
51109180029

Parent secondary phone:
47808060232
```

---

# 59. UX Principles

Приложение создаётся для одного основного пользователя — репетитора.

UI должен быть:

* быстрым;
* простым;
* desktop-first;
* без лишних экранов;
* удобным для ежедневной работы.

Основные действия должны занимать минимум кликов.

Главный navigation:

```text
Dashboard
Students
Calendar
Reports
Settings
```

---

# 60. MVP

Первая версия должна включать только:

### Authentication

* Google Login.

### Students

* список;
* создание вручную;
* импорт raw message;
* Preview;
* редактирование;
* удаление/архивация.

### Lessons

* создание;
* редактирование;
* отмена;
* Completed;
* No Show;
* связь с Student.

### Google Calendar

* выбор доступного календаря;
* создание событий;
* изменение;
* удаление;
* сохранение event ID.

### Reports

* выбор месяца;
* количество проведённых уроков;
* сумма;
* breakdown по ученикам.

### Google Sheets

* выбор существующей Spreadsheet;
* выбор Worksheet;
* экспорт monthly report.

### Deployment

* Docker;
* Railway;
* PostgreSQL.

---

# 61. What NOT to Build in MVP

Не реализовывать:

* Telegram bot;
* Messenger API;
* AI/LLM parser;
* automatic Messenger import;
* payment processing;
* invoices;
* parent portal;
* multiple tutors;
* complex roles/permissions;
* mobile application;
* two-way Google Calendar synchronization.

Архитектура должна позволять добавить эти функции позже, но они не должны усложнять MVP.

---

# 62. Development Order

Реализовывать проект в следующем порядке.

## Phase 1 — Project Setup

* Spring Boot;
* Angular;
* PostgreSQL;
* Docker;
* Liquibase;
* Railway configuration.

## Phase 2 — Authentication

* Google OAuth;
* User;
* Authentication;
* session management.

## Phase 3 — Student

* Student entity;
* CRUD;
* UI;
* Student Import Parser;
* Import Preview;
* duplicate detection.

## Phase 4 — Lessons

* Lesson entity;
* CRUD;
* status management;
* student history.

## Phase 5 — Google Calendar

* OAuth scopes;
* calendar discovery;
* calendar selection;
* event creation;
* event update;
* event deletion.

## Phase 6 — Reports

* monthly aggregation;
* student breakdown;
* earnings.

## Phase 7 — Google Sheets

* spreadsheet selection;
* worksheet selection;
* report export.

## Phase 8 — Dashboard

* current month statistics;
* upcoming lessons;
* active students.

---

# 63. Important Engineering Rules

## Do not over-engineer

Это небольшой personal productivity application.

Не добавлять микросервисы.

Не добавлять Kafka.

Не добавлять Redis без необходимости.

Не добавлять CQRS/Event Sourcing.

Один Spring Boot application + PostgreSQL достаточно.

---

## Keep integrations isolated

Google APIs не должны быть напрямую вызваны из controllers.

Правильно:

```text
Controller
    ↓
LessonService
    ↓
GoogleCalendarService
```

Неправильно:

```text
LessonController
    ↓
Google API
```

---

## Keep domain logic testable

Например, расчёт monthly report не должен зависеть от Google API.

```text
ReportService
    ↓
LessonRepository
    ↓
calculate report
```

А экспорт:

```text
ReportService
    ↓
GoogleSheetsService
```

---

# 64. Definition of Done

MVP считается готовым, если пользователь может:

1. Зайти через Google.
2. Вставить полученное из Messenger сообщение.
3. Получить распарсенный Preview.
4. Исправить данные.
5. Создать ученика.
6. Создать первый урок.
7. Увидеть урок в Google Calendar.
8. Создать последующие уроки.
9. Отметить уроки как Completed.
10. Открыть Monthly Report.
11. Увидеть количество проведённых уроков.
12. Увидеть сумму к выплате.
13. Выбрать существующий Google Sheet.
14. Экспортировать отчёт в Google Sheets.
15. Запустить приложение в production через Railway.

---

# 65. Final Principle

Главная идея приложения:

```text
Google Calendar = scheduling integration

Google Sheets = reporting/export integration

Tutor Management System = source of truth
```

Все данные о студентах, уроках и расчётах должны храниться в собственной PostgreSQL database.

Google Calendar и Google Sheets являются внешними интеграциями и не должны использоваться как основная база данных приложения.
