import { AfterViewInit, Component, ElementRef, OnDestroy, inject, viewChild } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Calendar, EventInput } from '@fullcalendar/core';
import dayGridPlugin from '@fullcalendar/daygrid';
import interactionPlugin from '@fullcalendar/interaction';
import timeGridPlugin from '@fullcalendar/timegrid';
import { firstValueFrom } from 'rxjs';
import { LessonDialogComponent, LessonDialogResult } from '../lessons/lesson-dialog.component';
import { Lesson, LESSON_STATUS_COLOR } from '../lessons/lesson.model';
import { LessonService } from '../lessons/lesson.service';

@Component({
  selector: 'app-calendar',
  template: `
    <div class="page">
      <div class="page-header"><h1>Calendar</h1></div>
      <div #cal class="calendar-host"></div>
    </div>
  `,
  styles: [
    `
      .calendar-host {
        background: #fff;
        padding: 12px;
        border-radius: 8px;
      }
      :host ::ng-deep .fc {
        font-size: 13px;
      }
      :host ::ng-deep .fc-event {
        cursor: pointer;
      }
    `,
  ],
})
export class CalendarComponent implements AfterViewInit, OnDestroy {
  private readonly lessons = inject(LessonService);
  private readonly dialog = inject(MatDialog);
  private readonly host = viewChild.required<ElementRef<HTMLDivElement>>('cal');

  private calendar?: Calendar;
  private cache = new Map<string, Lesson>();

  ngAfterViewInit(): void {
    this.calendar = new Calendar(this.host().nativeElement, {
      plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin],
      initialView: 'timeGridWeek',
      headerToolbar: {
        left: 'prev,next today',
        center: 'title',
        right: 'dayGridMonth,timeGridWeek,timeGridDay',
      },
      nowIndicator: true,
      firstDay: 1,
      height: 'auto',
      slotMinTime: '07:00:00',
      slotMaxTime: '23:00:00',
      datesSet: (info) => void this.loadRange(info.startStr, info.endStr),
      dateClick: (info) => this.openDialog({ start: info.date }),
      eventClick: (info) => {
        const lesson = this.cache.get(info.event.id);
        if (lesson) this.openDialog({ lesson });
      },
    });
    this.calendar.render();
  }

  ngOnDestroy(): void {
    this.calendar?.destroy();
  }

  private async loadRange(from: string, to: string): Promise<void> {
    const list = await firstValueFrom(this.lessons.list({ from, to }));
    this.cache.clear();
    const events: EventInput[] = list.map((l) => {
      this.cache.set(l.id, l);
      return {
        id: l.id,
        title: l.studentName ?? 'Lesson',
        start: l.startTime,
        end: l.endTime,
        backgroundColor: LESSON_STATUS_COLOR[l.status],
        borderColor: LESSON_STATUS_COLOR[l.status],
      };
    });
    this.calendar?.removeAllEvents();
    this.calendar?.addEventSource(events);
  }

  private openDialog(data: { lesson?: Lesson; start?: Date }): void {
    this.dialog
      .open<LessonDialogComponent, unknown, LessonDialogResult>(LessonDialogComponent, { data })
      .afterClosed()
      .subscribe((result) => {
        if (result) {
          const view = this.calendar?.view;
          if (view) void this.loadRange(view.activeStart.toISOString(), view.activeEnd.toISOString());
        }
      });
  }
}
