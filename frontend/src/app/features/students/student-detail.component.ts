import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { firstValueFrom } from 'rxjs';
import { ConfirmDialogComponent } from '../../core/ui/confirm-dialog.component';
import { SendEmailDialogComponent } from '../email/send-email-dialog.component';
import {
  ArchiveStudentDialogComponent,
  ArchiveStudentDialogData,
  ArchiveStudentDialogResult,
} from './archive-student-dialog.component';
import { LessonDialogComponent, LessonDialogResult } from '../lessons/lesson-dialog.component';
import { Lesson, LESSON_STATUS_LABEL, LessonStudentOverview } from '../lessons/lesson.model';
import { LessonService } from '../lessons/lesson.service';
import { dayLabel, formatLabel, statusLabel, telegramUrl } from './student.labels';
import { Student } from './student.model';
import { StudentService } from './student.service';

@Component({
  selector: 'app-student-detail',
  imports: [
    RouterLink,
    DatePipe,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatDividerModule,
    MatProgressBarModule,
    MatTooltipModule,
  ],
  templateUrl: './student-detail.component.html',
  styleUrl: './student-detail.component.scss',
})
export class StudentDetailComponent implements OnInit {
  private readonly service = inject(StudentService);
  private readonly lessonService = inject(LessonService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);

  readonly student = signal<Student | null>(null);
  readonly overview = signal<LessonStudentOverview | null>(null);
  readonly loading = signal(true);
  readonly syncing = signal(false);

  readonly statusLabel = statusLabel;
  readonly formatLabel = formatLabel;
  readonly dayLabel = dayLabel;
  readonly telegramUrl = telegramUrl;
  readonly lessonStatusLabel = LESSON_STATUS_LABEL;

  private get id(): string {
    return this.route.snapshot.paramMap.get('id')!;
  }

  ngOnInit(): void {
    this.service.get(this.id).subscribe({
      next: (s) => {
        this.student.set(s);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
    this.reloadLessons();
  }

  private reloadLessons(): void {
    this.lessonService.studentOverview(this.id).subscribe((o) => this.overview.set(o));
  }

  addLesson(s: Student): void {
    this.openLessonDialog({ studentId: s.id });
  }

  editLesson(l: Lesson): void {
    this.openLessonDialog({ lesson: l });
  }

  private openLessonDialog(data: { lesson?: Lesson; studentId?: string }): void {
    this.dialog
      .open<LessonDialogComponent, unknown, LessonDialogResult>(LessonDialogComponent, { data })
      .afterClosed()
      .subscribe((result) => {
        if (result) this.reloadLessons();
      });
  }

  async mark(l: Lesson, action: 'complete' | 'cancel' | 'no-show'): Promise<void> {
    const op =
      action === 'complete'
        ? this.lessonService.complete(l.id)
        : action === 'cancel'
          ? this.lessonService.cancel(l.id)
          : this.lessonService.noShow(l.id);
    await firstValueFrom(op);
    this.reloadLessons();
  }

  async retrySync(l: Lesson): Promise<void> {
    await firstValueFrom(this.lessonService.syncCalendar(l.id));
    this.reloadLessons();
  }

  async repeat(l: Lesson): Promise<void> {
    const raw = window.prompt('How many weekly lessons to add?', '4');
    const n = Number(raw);
    if (!Number.isFinite(n) || n < 1) return;
    await firstValueFrom(
      this.lessonService.repeat(l.id, { occurrences: Math.floor(n), intervalWeeks: 1 }),
    );
    this.reloadLessons();
  }

  /** Re-syncs just this student — handy after fixing a calendar permission or reconnecting Google. */
  async syncCalendarForStudent(student: Student): Promise<void> {
    this.syncing.set(true);
    try {
      const result = await firstValueFrom(this.lessonService.syncStudentCalendar(student.id));
      if (!result.enabled) {
        this.snack.open('Connect Google and pick a calendar in Settings first', 'Dismiss', {
          duration: 6000,
        });
      } else {
        const parts = [`${result.synced} synced`];
        if (result.removed) parts.push(`${result.removed} removed`);
        if (result.failed) parts.push(`${result.failed} failed`);
        this.snack.open(parts.join(', '), 'Dismiss', { duration: 5000 });
      }
      this.reloadLessons();
    } finally {
      this.syncing.set(false);
    }
  }

  /** Manual send: pick a template, review the rendered message, then send from Gmail. */
  sendEmail(student: Student, lessonId?: string): void {
    this.dialog
      .open(SendEmailDialogComponent, {
        data: {
          studentId: student.id,
          studentName: `${student.firstName} ${student.lastName}`,
          lessonId,
          defaultRecipient: student.email ?? student.parentEmail,
        },
        maxWidth: '90vw',
      })
      .afterClosed()
      .subscribe((result) => {
        if (result) {
          this.snack.open(`Email sent to ${result.to}`, 'Dismiss', { duration: 5000 });
        }
      });
  }

  async archive(s: Student): Promise<void> {
    const result = await firstValueFrom(
      this.dialog
        .open<ArchiveStudentDialogComponent, ArchiveStudentDialogData, ArchiveStudentDialogResult>(
          ArchiveStudentDialogComponent,
          {
            data: { studentName: `${s.firstName} ${s.lastName}`, reason: s.leaveReason },
          },
        )
        .afterClosed(),
    );
    if (!result) return;
    this.student.set(await firstValueFrom(this.service.archive(s.id, result.reason)));
  }

  async remove(s: Student): Promise<void> {
    const ok = await this.confirm(`Delete ${s.firstName} ${s.lastName}?`, 'This cannot be undone.');
    if (!ok) return;
    await firstValueFrom(this.service.delete(s.id));
    await this.router.navigate(['/students']);
  }

  private confirm(title: string, message: string): Promise<boolean> {
    return firstValueFrom(
      this.dialog
        .open(ConfirmDialogComponent, { data: { title, message }, width: '420px' })
        .afterClosed(),
    ).then((v) => v === true);
  }
}
