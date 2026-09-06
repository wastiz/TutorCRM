import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { MatBadgeModule } from '@angular/material/badge';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Calendar, EventInput } from '@fullcalendar/core';
import dayGridPlugin from '@fullcalendar/daygrid';
import interactionPlugin from '@fullcalendar/interaction';
import timeGridPlugin from '@fullcalendar/timegrid';
import { firstValueFrom } from 'rxjs';
import { GoogleImportDialogComponent } from '../lessons/google-import-dialog.component';
import { LessonDialogComponent, LessonDialogResult } from '../lessons/lesson-dialog.component';
import { GoogleImportPreview, Lesson, LESSON_STATUS_COLOR } from '../lessons/lesson.model';
import { LessonService } from '../lessons/lesson.service';

@Component({
  selector: 'app-calendar',
  imports: [MatButtonModule, MatIconModule, MatBadgeModule, MatSlideToggleModule],
  templateUrl: './calendar.component.html',
  styleUrl: './calendar.component.scss',
})
export class CalendarComponent implements AfterViewInit, OnDestroy {
  private readonly lessons = inject(LessonService);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);
  private readonly host = viewChild.required<ElementRef<HTMLDivElement>>('cal');
  private readonly breakpoints = inject(BreakpointObserver);

  private calendar?: Calendar;
  private cache = new Map<string, Lesson>();
  private range = { from: '', to: '' };

  readonly importCount = signal(0);
  /** Lessons that did not happen are hidden — they are also deleted from Google Calendar. */
  readonly showCancelled = signal(false);
  private lastPreview: GoogleImportPreview | null = null;

  ngAfterViewInit(): void {
    // a week grid is unreadable on a phone — start on the day view with a compact toolbar
    const handset = this.breakpoints.isMatched(Breakpoints.Handset);
    this.calendar = new Calendar(this.host().nativeElement, {
      plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin],
      initialView: handset ? 'timeGridDay' : 'timeGridWeek',
      headerToolbar: handset
        ? { left: 'prev,next', center: 'title', right: 'timeGridDay,dayGridMonth' }
        : {
            left: 'prev,next today',
            center: 'title',
            right: 'dayGridMonth,timeGridWeek,timeGridDay',
          },
      nowIndicator: true,
      firstDay: 1,
      height: 'auto',
      slotMinTime: '07:00:00',
      slotMaxTime: '23:00:00',
      datesSet: (info) => {
        this.range = { from: info.start.toISOString(), to: info.end.toISOString() };
        void this.loadRange();
        void this.refreshImportBadge();
      },
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

  toggleCancelled(show: boolean): void {
    this.showCancelled.set(show);
    void this.loadRange();
  }

  private async loadRange(): Promise<void> {
    const list = await firstValueFrom(this.lessons.list(this.range));
    this.cache.clear();
    const visible = this.showCancelled()
      ? list
      : list.filter((l) => l.status !== 'CANCELLED' && l.status !== 'NO_SHOW');
    const events: EventInput[] = visible.map((l) => {
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

  private async refreshImportBadge(): Promise<void> {
    try {
      const preview = await firstValueFrom(
        this.lessons.googleImportPreview(this.range.from, this.range.to),
      );
      this.lastPreview = preview;
      this.importCount.set(preview.newEvents.length + preview.movedLessons.length);
    } catch {
      this.lastPreview = null;
      this.importCount.set(0);
    }
  }

  async openImport(): Promise<void> {
    const preview =
      this.lastPreview ??
      (await firstValueFrom(this.lessons.googleImportPreview(this.range.from, this.range.to)));

    if (!preview.enabled) {
      this.snack.open('Connect Google and pick a calendar in Settings first', 'Settings', {
        duration: 6000,
      });
      return;
    }

    this.dialog
      .open(GoogleImportDialogComponent, { data: { preview }, maxWidth: '90vw' })
      .afterClosed()
      .subscribe((result) => {
        if (result) {
          this.snack.open(
            `Imported ${result.imported} lesson(s)` +
              (result.updated ? `, updated ${result.updated}` : ''),
            'Dismiss',
            { duration: 5000 },
          );
          void this.loadRange();
          void this.refreshImportBadge();
        }
      });
  }

  private openDialog(data: { lesson?: Lesson; start?: Date }): void {
    this.dialog
      .open<LessonDialogComponent, unknown, LessonDialogResult>(LessonDialogComponent, { data })
      .afterClosed()
      .subscribe((result) => {
        if (result) void this.loadRange();
      });
  }
}
