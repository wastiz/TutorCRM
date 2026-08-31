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
import { firstValueFrom } from 'rxjs';
import { ConfirmDialogComponent } from '../../core/ui/confirm-dialog.component';
import { LessonDialogComponent, LessonDialogResult } from '../lessons/lesson-dialog.component';
import { Lesson, LESSON_STATUS_LABEL, LessonStudentOverview } from '../lessons/lesson.model';
import { LessonService } from '../lessons/lesson.service';
import { dayLabel, formatLabel, statusLabel } from './student.labels';
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
  ],
  template: `
    <div class="page">
      @if (loading()) {
        <mat-progress-bar mode="indeterminate" />
      } @else if (student(); as s) {
        <div class="page-header">
          <h1>#{{ s.studentNumber }} · {{ s.firstName }} {{ s.lastName }}</h1>
          <div class="header-actions">
            <button mat-flat-button color="primary" (click)="addLesson(s)">
              <mat-icon>add</mat-icon> Add lesson
            </button>
            <a mat-stroked-button [routerLink]="['/students', s.id, 'edit']">
              <mat-icon>edit</mat-icon> Edit
            </a>
            @if (s.status !== 'FINISHED') {
              <button mat-stroked-button (click)="archive(s)"><mat-icon>archive</mat-icon> Archive</button>
            }
            <button mat-stroked-button color="warn" (click)="remove(s)">
              <mat-icon>delete</mat-icon> Delete
            </button>
          </div>
        </div>

        @if (overview(); as o) {
          <div class="stats">
            <div class="stat"><span class="value">{{ o.completedThisMonth }}</span><span class="label">Completed this month</span></div>
            <div class="stat"><span class="value">€{{ o.earningsThisMonth }}</span><span class="label">Earnings this month</span></div>
            <div class="stat"><span class="value">€{{ o.earningsTotal }}</span><span class="label">Total earnings</span></div>
          </div>
        }

        <div class="cards">
          <mat-card>
            <mat-card-header><mat-card-title>Student information</mat-card-title></mat-card-header>
            <mat-card-content>
              <dl>
                <dt>Status</dt><dd>{{ statusLabel(s.status) }}</dd>
                <dt>Email</dt><dd>{{ s.email || '—' }}</dd>
                <dt>Phone</dt><dd>{{ s.phone || '—' }}</dd>
                <dt>Isikukood</dt><dd>{{ s.isikukood || '—' }}</dd>
                <dt>Age</dt><dd>{{ s.age ?? '—' }}</dd>
                <dt>Grade</dt><dd>{{ s.grade ?? '—' }}</dd>
                <dt>School</dt><dd>{{ s.school || '—' }}</dd>
                <dt>Subject</dt><dd>{{ s.subject || '—' }}</dd>
                <dt>Level</dt><dd>{{ s.level || '—' }}</dd>
                <dt>Format</dt><dd>{{ formatLabel(s.lessonFormat) }}</dd>
                <dt>Lesson price</dt><dd>{{ s.lessonPrice != null ? ('€' + s.lessonPrice) : '—' }}</dd>
                <dt>Goal</dt><dd>{{ s.goal || '—' }}</dd>
                <dt>Notes</dt><dd>{{ s.notes || '—' }}</dd>
              </dl>
            </mat-card-content>
          </mat-card>

          <mat-card>
            <mat-card-header><mat-card-title>Schedule</mat-card-title></mat-card-header>
            <mat-card-content>
              @if (s.schedules.length) {
                <ul class="plain">
                  @for (row of s.schedules; track $index) {
                    <li>
                      {{ dayLabel(row.dayOfWeek) }}
                      @if (row.startTime) { · {{ row.startTime }}–{{ row.endTime }} }
                      @if (row.lessonsPerWeek) { · {{ row.lessonsPerWeek }}/week }
                    </li>
                  }
                </ul>
              } @else {
                <p class="muted">No preferred schedule set.</p>
              }
            </mat-card-content>
          </mat-card>

          <mat-card>
            <mat-card-header><mat-card-title>Parent information</mat-card-title></mat-card-header>
            <mat-card-content>
              <dl>
                <dt>Name</dt><dd>{{ s.parentName || '—' }}</dd>
                <dt>Phone</dt><dd>{{ s.parentPhone || '—' }}</dd>
                <dt>Secondary phone</dt><dd>{{ s.parentSecondaryPhone || '—' }}</dd>
                <dt>Email</dt><dd>{{ s.parentEmail || '—' }}</dd>
              </dl>
            </mat-card-content>
          </mat-card>

          <mat-card class="span-2">
            <mat-card-header><mat-card-title>Upcoming lessons</mat-card-title></mat-card-header>
            <mat-card-content>
              @if (overview()?.upcoming?.length) {
                @for (l of overview()!.upcoming; track l.id) {
                  <div class="lesson-row">
                    <span>{{ l.startTime | date: 'EEE d MMM, HH:mm' }} – {{ l.endTime | date: 'HH:mm' }}</span>
                    <span class="muted">€{{ l.price }}</span>
                    <span class="spacer"></span>
                    <button mat-icon-button [matMenuTriggerFor]="menu"><mat-icon>more_vert</mat-icon></button>
                    <mat-menu #menu="matMenu">
                      <button mat-menu-item (click)="editLesson(l)"><mat-icon>edit</mat-icon> Edit</button>
                      <button mat-menu-item (click)="mark(l, 'complete')"><mat-icon>check_circle</mat-icon> Mark completed</button>
                      <button mat-menu-item (click)="mark(l, 'no-show')"><mat-icon>person_off</mat-icon> No show</button>
                      <button mat-menu-item (click)="mark(l, 'cancel')"><mat-icon>cancel</mat-icon> Cancel</button>
                      <button mat-menu-item (click)="repeat(l)"><mat-icon>repeat</mat-icon> Repeat weekly…</button>
                    </mat-menu>
                  </div>
                }
              } @else {
                <p class="muted">No upcoming lessons.</p>
              }
            </mat-card-content>
          </mat-card>

          <mat-card class="span-2">
            <mat-card-header><mat-card-title>Past lessons</mat-card-title></mat-card-header>
            <mat-card-content>
              @if (overview()?.past?.length) {
                @for (l of overview()!.past; track l.id) {
                  <div class="lesson-row">
                    <span>{{ l.startTime | date: 'EEE d MMM yyyy, HH:mm' }}</span>
                    <span class="badge" [class]="'st-' + l.status.toLowerCase()">{{ lessonStatusLabel[l.status] }}</span>
                    <span class="muted">€{{ l.price }}</span>
                    <span class="spacer"></span>
                    <button mat-button (click)="editLesson(l)">Edit</button>
                  </div>
                }
              } @else {
                <p class="muted">No past lessons.</p>
              }
            </mat-card-content>
          </mat-card>
        </div>
      }
    </div>
  `,
  styles: [
    `
      .header-actions { display: flex; gap: 8px; flex-wrap: wrap; }
      .cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }
      .span-2 { grid-column: 1 / -1; }
      dl { display: grid; grid-template-columns: 130px 1fr; row-gap: 6px; margin: 0; }
      dt { color: rgba(0, 0, 0, 0.55); }
      ul.plain { margin: 0; padding-left: 18px; }
      .stats { display: flex; gap: 16px; margin: 8px 0 20px; flex-wrap: wrap; }
      .stat { background: #fff; border-radius: 8px; padding: 14px 20px; min-width: 160px; display: flex; flex-direction: column; }
      .stat .value { font-size: 22px; font-weight: 600; }
      .stat .label { font-size: 12px; color: rgba(0, 0, 0, 0.55); }
      .lesson-row { display: flex; align-items: center; gap: 12px; padding: 6px 0; border-bottom: 1px solid #f0f0f0; }
      .spacer { flex: 1 1 auto; }
      .badge { padding: 2px 8px; border-radius: 10px; font-size: 11px; }
      .st-completed { background: #e8f5e9; color: #2e7d32; }
      .st-planned { background: #e8eaf6; color: #3949ab; }
      .st-cancelled { background: #eceff1; color: #607d8b; }
      .st-no_show { background: #ffebee; color: #c62828; }
    `,
  ],
})
export class StudentDetailComponent implements OnInit {
  private readonly service = inject(StudentService);
  private readonly lessonService = inject(LessonService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);

  readonly student = signal<Student | null>(null);
  readonly overview = signal<LessonStudentOverview | null>(null);
  readonly loading = signal(true);

  readonly statusLabel = statusLabel;
  readonly formatLabel = formatLabel;
  readonly dayLabel = dayLabel;
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

  async repeat(l: Lesson): Promise<void> {
    const raw = window.prompt('How many weekly lessons to add?', '4');
    const n = Number(raw);
    if (!Number.isFinite(n) || n < 1) return;
    await firstValueFrom(this.lessonService.repeat(l.id, { occurrences: Math.floor(n), intervalWeeks: 1 }));
    this.reloadLessons();
  }

  async archive(s: Student): Promise<void> {
    const ok = await this.confirm(`Archive ${s.firstName} ${s.lastName}?`, 'They will be marked as finished.');
    if (!ok) return;
    this.student.set(await firstValueFrom(this.service.archive(s.id)));
  }

  async remove(s: Student): Promise<void> {
    const ok = await this.confirm(`Delete ${s.firstName} ${s.lastName}?`, 'This cannot be undone.');
    if (!ok) return;
    await firstValueFrom(this.service.delete(s.id));
    await this.router.navigate(['/students']);
  }

  private confirm(title: string, message: string): Promise<boolean> {
    return firstValueFrom(
      this.dialog.open(ConfirmDialogComponent, { data: { title, message }, width: '420px' }).afterClosed(),
    ).then((v) => v === true);
  }
}
