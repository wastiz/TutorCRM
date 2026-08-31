import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { firstValueFrom } from 'rxjs';
import { StudentSummary } from '../students/student.model';
import { StudentService } from '../students/student.service';
import { Lesson, LessonRequest, LessonStatus } from './lesson.model';
import { LessonService } from './lesson.service';

export interface LessonDialogData {
  lesson?: Lesson;
  studentId?: string;
  start?: Date;
}

export type LessonDialogResult = { action: 'saved'; lesson: Lesson } | { action: 'deleted' };

function toLocalInput(iso: string | Date): string {
  const d = typeof iso === 'string' ? new Date(iso) : iso;
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

@Component({
  selector: 'app-lesson-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatCheckboxModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ data.lesson ? 'Edit lesson' : 'New lesson' }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="form">
        <mat-form-field>
          <mat-label>Student</mat-label>
          <mat-select formControlName="studentId">
            @for (s of students(); track s.id) {
              <mat-option [value]="s.id">#{{ s.studentNumber }} · {{ s.fullName }}</mat-option>
            }
          </mat-select>
        </mat-form-field>

        <mat-form-field>
          <mat-label>Start</mat-label>
          <input matInput type="datetime-local" formControlName="start" />
        </mat-form-field>

        <mat-form-field>
          <mat-label>Duration (minutes)</mat-label>
          <mat-select formControlName="durationMinutes">
            @for (m of durations; track m) {
              <mat-option [value]="m">{{ m }}</mat-option>
            }
          </mat-select>
        </mat-form-field>

        <mat-form-field>
          <mat-label>Price (€)</mat-label>
          <input matInput type="number" step="0.01" formControlName="price" placeholder="student default" />
        </mat-form-field>

        @if (data.lesson) {
          <mat-form-field>
            <mat-label>Status</mat-label>
            <mat-select formControlName="status">
              @for (s of statuses; track s) {
                <mat-option [value]="s">{{ s }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
        }

        <mat-form-field class="wide">
          <mat-label>Notes</mat-label>
          <textarea matInput rows="2" formControlName="notes"></textarea>
        </mat-form-field>

        @if (!data.lesson) {
          <mat-checkbox formControlName="repeat">Also create recurring lessons</mat-checkbox>
          @if (form.controls.repeat.value) {
            <div class="repeat-row">
              <mat-form-field>
                <mat-label>Extra lessons</mat-label>
                <input matInput type="number" formControlName="occurrences" />
              </mat-form-field>
              <mat-form-field>
                <mat-label>Every N weeks</mat-label>
                <input matInput type="number" formControlName="intervalWeeks" />
              </mat-form-field>
            </div>
          }
        }
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      @if (data.lesson) {
        <button mat-button color="warn" (click)="remove()">Delete</button>
      }
      <span class="spacer"></span>
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid || busy()" (click)="save()">
        Save
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .form {
        display: flex;
        flex-direction: column;
        gap: 8px;
        min-width: 420px;
      }
      .wide {
        width: 100%;
      }
      .repeat-row {
        display: flex;
        gap: 12px;
      }
      .spacer {
        flex: 1 1 auto;
      }
    `,
  ],
})
export class LessonDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly lessons = inject(LessonService);
  private readonly studentService = inject(StudentService);
  private readonly ref = inject(MatDialogRef<LessonDialogComponent, LessonDialogResult>);
  readonly data = inject<LessonDialogData>(MAT_DIALOG_DATA);

  readonly durations = [30, 45, 60, 90, 120];
  readonly statuses: LessonStatus[] = ['PLANNED', 'COMPLETED', 'CANCELLED', 'NO_SHOW'];
  readonly students = signal<StudentSummary[]>([]);
  readonly busy = signal(false);

  readonly form = this.fb.group({
    studentId: [this.data.lesson?.studentId ?? this.data.studentId ?? '', Validators.required],
    start: [
      toLocalInput(this.data.lesson?.startTime ?? this.data.start ?? roundToNextHour()),
      Validators.required,
    ],
    durationMinutes: [this.initialDuration(), Validators.required],
    price: [this.data.lesson?.price ?? null as number | null],
    status: [this.data.lesson?.status ?? ('PLANNED' as LessonStatus)],
    notes: [this.data.lesson?.notes ?? ''],
    repeat: [false],
    occurrences: [3],
    intervalWeeks: [1],
  });

  constructor() {
    this.studentService.list().subscribe((list) => this.students.set(list));
  }

  private initialDuration(): number {
    if (this.data.lesson) {
      const mins =
        (new Date(this.data.lesson.endTime).getTime() - new Date(this.data.lesson.startTime).getTime()) /
        60000;
      return this.durations.includes(mins) ? mins : 60;
    }
    return 60;
  }

  async save(): Promise<void> {
    this.busy.set(true);
    try {
      const v = this.form.getRawValue();
      const start = new Date(v.start!);
      const end = new Date(start.getTime() + v.durationMinutes! * 60000);
      const request: LessonRequest = {
        studentId: v.studentId!,
        startTime: start.toISOString(),
        endTime: end.toISOString(),
        price: v.price === null || v.price === undefined ? null : Number(v.price),
        notes: v.notes || null,
        status: this.data.lesson ? v.status : null,
      };

      const saved = this.data.lesson
        ? await firstValueFrom(this.lessons.update(this.data.lesson.id, request))
        : await firstValueFrom(this.lessons.create(request));

      if (!this.data.lesson && v.repeat) {
        await firstValueFrom(
          this.lessons.repeat(saved.id, {
            occurrences: Number(v.occurrences) || 1,
            intervalWeeks: Number(v.intervalWeeks) || 1,
          }),
        );
      }
      this.ref.close({ action: 'saved', lesson: saved });
    } finally {
      this.busy.set(false);
    }
  }

  async remove(): Promise<void> {
    if (!this.data.lesson) return;
    this.busy.set(true);
    try {
      await firstValueFrom(this.lessons.delete(this.data.lesson.id));
      this.ref.close({ action: 'deleted' });
    } finally {
      this.busy.set(false);
    }
  }
}

function roundToNextHour(): Date {
  const d = new Date();
  d.setMinutes(0, 0, 0);
  d.setHours(d.getHours() + 1);
  return d;
}
