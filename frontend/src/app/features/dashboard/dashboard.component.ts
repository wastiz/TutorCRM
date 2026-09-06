import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { LESSON_STATUS_LABEL, Lesson, LessonStatus } from '../lessons/lesson.model';
import { LessonService } from '../lessons/lesson.service';
import { Dashboard } from './dashboard.model';
import { DashboardService } from './dashboard.service';

@Component({
  selector: 'app-dashboard',
  imports: [
    RouterLink,
    DatePipe,
    DecimalPipe,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatListModule,
    MatProgressBarModule,
    MatTooltipModule,
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit {
  private readonly service = inject(DashboardService);
  private readonly lessons = inject(LessonService);
  private readonly snack = inject(MatSnackBar);

  readonly data = signal<Dashboard | null>(null);
  readonly loading = signal(true);
  readonly busyLesson = signal<string | null>(null);
  readonly today = signal(new Date());

  readonly statusLabel = (s: LessonStatus) => LESSON_STATUS_LABEL[s];

  ngOnInit(): void {
    this.reload();
  }

  durationLabel(lesson: Lesson): string {
    const minutes = Math.round(
      (new Date(lesson.endTime).getTime() - new Date(lesson.startTime).getTime()) / 60000,
    );
    return minutes % 60 === 0 ? `${minutes / 60} h` : `${minutes} min`;
  }

  /** Mark a lesson off; a lesson that did not happen also loses its Google Calendar event. */
  async mark(lesson: Lesson, action: 'complete' | 'no-show' | 'cancel' | 'undo'): Promise<void> {
    this.busyLesson.set(lesson.id);
    try {
      const updated = await firstValueFrom(
        action === 'complete'
          ? this.lessons.complete(lesson.id)
          : action === 'no-show'
            ? this.lessons.noShow(lesson.id)
            : action === 'cancel'
              ? this.lessons.cancel(lesson.id)
              : this.lessons.replan(lesson.id),
      );
      this.patchLesson(updated);
      if (action === 'no-show' || action === 'cancel') {
        this.snack.open('Lesson removed from the calendar', 'Dismiss', { duration: 4000 });
      }
    } finally {
      this.busyLesson.set(null);
    }
  }

  private patchLesson(updated: Lesson): void {
    const current = this.data();
    if (!current) return;
    this.data.set({
      ...current,
      todaysLessonList: current.todaysLessonList.map((l) => (l.id === updated.id ? updated : l)),
    });
    // the tiles (earnings, completed count) depend on the change too
    this.service.summary().subscribe((d) => this.data.set(d));
  }

  private reload(): void {
    this.service.summary().subscribe({
      next: (d) => {
        this.data.set(d);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
