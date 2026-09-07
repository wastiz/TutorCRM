Monthly Report — Google Sheets Specification
1. Purpose

Monthly Report должен генерироваться в формате, максимально соответствующем существующей рабочей Google Sheets таблице репетитора.

Google Sheets используется как официальный формат monthly payout report.

Приложение не должно придумывать собственный формат отчёта.

2. Student Fields Required for Report

Для корректной генерации отчёта Student должен содержать следующие поля:

UUID id;

UUID userId;

String firstName;

String lastName;

String studentNumber;

String email;

String parentName;

String parentPhone;

String isikukood;

String subject;

BigDecimal lessonPrice;

studentNumber хранить как String, а не Integer.

isikukood хранить как String, а не Integer.

Причина:

возможны ведущие нули;
это идентификаторы, а не числа для математических операций.
3. Report Format

Текущий рабочий формат:

August 26

Имя фамилия латиницей | Номер уч. | Почта | Имя родителя | Isikukood | Предмет | Количество уроков | Цена урока | Сумма | Итого

Для августа 2026 название должно быть:

август 26

Использовать русские названия месяцев:

январь
февраль
март
апрель
май
июнь
июль
август
сентябрь
октябрь
ноябрь
декабрь

Формат:

{месяц на русском} {две последние цифры года}

Например:

август 26
сентябрь 26
октябрь 26
4. Spreadsheet Columns

Колонки должны идти строго в следующем порядке:

A: Имя фамилия латиницей
B: Номер уч.
C: Почта
D: Имя родителя
E: Isikukood
F: Предмет
G: Количество уроков
H: Цена урока
I: Сумма
J: Итого

Не менять порядок колонок.

5. Example

Для данных:

Pavel Kuznetsov
studentNumber = 1
email = pavel.kuznetsov@example.com
parentName = null
isikukood = 39709250002
subject = Эстонский
completedLessons = 1
lessonPrice = 12

строка должна выглядеть:

Pavel Kuznetsov | 1 | pavel.kuznetsov@example.com | | 39709250002 | Эстонский | 1 | 12 | 12 |
6. Total Calculation

Сумма для каждого ученика:

Количество уроков × Цена урока

Например:

1 × 12 = 12

2 × 18 = 36
7. Monthly Total

В колонке J должна отображаться общая сумма за месяц.

Общий итог:

SUM(all student totals)

Например:

Student               Lessons   Price   Sum
--------------------------------------------
Pavel Kuznetsov          1       12      12
Konstantin Larin         1       12      12
Вероника                 1       12      12
Артур                    2       18      36
Яна Лебедева             1       12      12
Timur Karu               1       12      12
--------------------------------------------
TOTAL                    7               96

В существующем шаблоне значение 96 располагается в колонке Итого.

Не дублировать 96 в каждой строке.

8. Important: Report Is Based on Lessons

Количество уроков должно вычисляться исключительно из Lesson.

Для выбранного месяца учитывать:

Lesson.status = COMPLETED

и:

startTime >= firstDayOfMonth
startTime < firstDayOfNextMonth

Не учитывать:

PLANNED
CANCELLED
NO_SHOW
9. Price

Цена должна браться из самого Lesson:

Lesson.price

Не брать текущий:

Student.lessonPrice

Причина:

цена ученика может измениться со временем.

Например:

August:
8 lessons × €12 = €96

September:
8 lessons × €15 = €120

Изменение цены в September не должно менять August report.

10. Grouping

Уроки необходимо группировать по Student.

SQL/business logic concept:

GROUP BY student_id

Для каждого Student получить:

completedLessonsCount
lessonPrice
totalAmount
11. Multiple Prices for One Student

Если в течение одного месяца у одного ученика встречаются разные Lesson.price, нельзя молча выбрать одну цену.

Например:

August:

Lesson 1 → €12
Lesson 2 → €12
Lesson 3 → €15

В таком случае Report Service должен вернуть warning:

Student X has multiple lesson prices in August.

Для MVP не пытаться автоматически объединять такие значения.

В UI необходимо показать проблему перед экспортом.

12. Report Preview

Перед экспортом в Google Sheets пользователь должен увидеть Preview.

Пример:

август 26

Имя фамилия латиницей | Номер уч. | Почта | Имя родителя | Isikukood | Предмет | Количество уроков | Цена урока | Сумма | Итого

Pavel Kuznetsov       | 1  | pavel.kuznetsov@example.com |        | 39709250002 | Эстонский  | 1 | 12 | 12 |
Konstantin Larin      | 3  | irina.larina@example.com    | Ирина  | 51011010001 | Эстонский | 1 | 12 | 12 |
Вероника              | 6  | marina.kask@example.com     | Марина | 61701090009 | Эстонский | 1 | 12 | 12 |
Артур                 | 8  |                             |        | 61701090009 | Эстонский | 2 | 18 | 36 |
Яна Лебедева          | 9  |                             |        | 51004090002 | Эстонский | 1 | 12 | 12 |
Timur Karu            | 11 | timur.karu@example.com      | Marika | 51004090002 | Математика | 1 | 12 | 12 |

                                                                                  7       96

Пользователь должен подтвердить экспорт.

[Export to Google Sheets]
13. Google Sheets Export

Создать:

report/
ReportController
ReportService
ReportRepository
Report
ReportDto

integration/
google/
sheets/
GoogleSheetsService
GoogleSheetsClient
GoogleSheetsReportExporter

ReportService отвечает за:

получение Lessons;
фильтрацию;
grouping;
calculation;
validation;
формирование report DTO.

GoogleSheetsService отвечает только за:

Google Sheets API;
поиск spreadsheet;
поиск worksheet;
запись данных;
форматирование таблицы.
14. Google Sheets Export Algorithm
    User selects month
    ↓
    ReportService.generate(month)
    ↓
    Find COMPLETED lessons
    ↓
    Group by Student
    ↓
    Calculate lesson count
    ↓
    Calculate total per student
    ↓
    Calculate monthly total
    ↓
    Validate report
    ↓
    Show Preview
    ↓
    User confirms
    ↓
    GoogleSheetsService.export(report)
15. Spreadsheet Layout

При экспорте:

Row 1

Название месяца:

август 26
Row 2

Headers:

Имя фамилия латиницей
Номер уч.
Почта
Имя родителя
Isikukood
Предмет
Количество уроков
Цена урока
Сумма
Итого
Rows 3..N

Students.

Final row

Monthly total.

Например:

август 26

Имя фамилия латиницей | Номер уч. | Почта | Имя родителя | Isikukood | Предмет | Количество уроков | Цена урока | Сумма | Итого

Pavel Kuznetsov       | 1  | ... |        | 39709250002 | Эстонский  | 1 | 12 | 12 |
Konstantin Larin      | 3  | ... | Ирина  | 51011010001 | Эстонский | 1 | 12 | 12 |
Вероника              | 6  | ... | Марина | 61701090009 | Эстонский | 1 | 12 | 12 |
Артур                 | 8  | ... |        | 61701090009 | Эстонский | 2 | 18 | 36 |
Яна Лебедева          | 9  | ... |        | 51004090002 | Эстонский | 1 | 12 | 12 |
Timur Karu            | 11 | ... | Marika | 51004090002 | Математика | 1 | 12 | 12 |

                                                                  7 | | | 96
16. Spreadsheet Formatting

После записи данных Google Sheets API должен применить базовое форматирование.

Минимально:

bold для header;
bold для monthly total;
borders для таблицы;
автоматическая ширина колонок;
числовой формат для Количество уроков;
числовой формат для Цена урока;
числовой формат для Сумма;
числовой формат для Итого.

Не использовать сложный дизайн.

Главная цель — максимально близко воспроизвести существующую рабочую таблицу.

17. Existing Spreadsheet

Экспорт должен поддерживать существующий Google Spreadsheet.

Не создавать новый Spreadsheet при каждом экспорте.

Пользователь один раз выбирает:

Spreadsheet
Worksheet

После чего настройки сохраняются.

Например:

Google Spreadsheet:
Tutor Payments 2026

Worksheet:
Payments
18. Monthly Worksheet Strategy

MVP должен поддерживать один из двух вариантов:

Option A

Каждый месяц записывать отчёт в новый worksheet:

январь 26
февраль 26
март 26
...
август 26
Option B

Использовать существующий worksheet.

Предпочтительный вариант для MVP:

создавать отдельный worksheet для каждого месяца.

Например:

Tutor Payments 2026
│
├── январь 26
├── февраль 26
├── март 26
├── апрель 26
├── май 26
├── июнь 26
├── июль 26
└── август 26

Если worksheet уже существует, не создавать дубликат.

Вместо этого спросить/предложить:

Worksheet "август 26" already exists.

[Update existing]
[Cancel]
19. Report DTO

Использовать отдельные DTO.

Например:

public record MonthlyReportDto(
YearMonth month,
List<StudentReportRowDto> students,
int totalLessons,
BigDecimal totalAmount
) {}
public record StudentReportRowDto(
String fullName,
String studentNumber,
String email,
String parentName,
String isikukood,
String subject,
int lessonCount,
BigDecimal lessonPrice,
BigDecimal total
) {}
20. Report Sorting

По умолчанию сортировать студентов по:

studentNumber ASC

То есть:

1
3
6
8
9
11

Это соответствует текущему формату отчёта.

21. Missing Data

Если у Student отсутствует поле:

email
parentName
isikukood

не блокировать создание отчёта.

Показывать пустую ячейку.

Например:

Артур | 8 | | | 61701090009 | Эстонский | 2 | 18 | 36
22. Report Validation

Перед экспортом проверить:

у каждого Lesson существует Student;
completed lesson имеет price;
Student имеет studentNumber, если он обязателен для текущего отчёта;
нет конфликтующих цен;
total корректно рассчитан.

Ошибки блокируют экспорт.

Warnings могут позволять экспорт после подтверждения.

23. Source of Truth

Архитектурное правило:

PostgreSQL
↓
Source of Truth

Google Calendar
↓
Scheduling Integration

Google Sheets
↓
Reporting / Export

Google Sheets не является источником расчётов.

Все суммы должны рассчитываться в backend из PostgreSQL.

24. Important

Не пытаться читать количество проведённых уроков обратно из Google Sheets.

Не пытаться вычислять earnings на основании существующей таблицы.

Правильный flow:

Lesson database
↓
ReportService
↓
Monthly Report
↓
Google Sheets

Google Sheets является конечным результатом экспорта.