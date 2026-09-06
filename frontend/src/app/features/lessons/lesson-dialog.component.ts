import { Component, computed, effect, inject, signal } from '@angular/core';
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
  templateUrl: './lesson-dialog.component.html',
  styleUrl: './lesson-dialog.component.scss',
})
export class LessonDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly lessons = inject(LessonService);
  private readonly studentService = inject(StudentService);
  private readonly ref = inject(MatDialogRef<LessonDialogComponent, LessonDialogResult>);
  readonly data = inject<LessonDialogData>(MAT_DIALOG_DATA);

  /** CLAUDE.md-era lessons could be any length; the standard offer is 1 / 1.5 / 2 hours. */
  private static readonly STANDARD_DURATIONS = [60, 90, 120];
  readonly statuses: LessonStatus[] = ['PLANNED', 'COMPLETED', 'CANCELLED', 'NO_SHOW'];
  readonly students = signal<StudentSummary[]>([]);
  readonly busy = signal(false);
  /** Mirror the controls the computed price depends on (templates cannot read private fields). */
  protected readonly selectedStudentId = signal<string>(
    this.data.lesson?.studentId ?? this.data.studentId ?? '',
  );
  protected readonly durationMinutes = signal<number>(this.initialDuration());
  protected readonly customPrice = signal(false);

  readonly form = this.fb.group({
    studentId: [this.data.lesson?.studentId ?? this.data.studentId ?? '', Validators.required],
    start: [
      toLocalInput(this.data.lesson?.startTime ?? this.data.start ?? roundToNextHour()),
      Validators.required,
    ],
    durationMinutes: [this.initialDuration(), Validators.required],
    price: [{ value: this.data.lesson?.price ?? null, disabled: true } as unknown as number | null],
    customPrice: [false],
    status: [this.data.lesson?.status ?? ('PLANNED' as LessonStatus)],
    notes: [this.data.lesson?.notes ?? ''],
    repeat: [false],
    occurrences: [3],
    intervalWeeks: [1],
  });

  constructor() {
    this.studentService.list().subscribe((list) => {
      this.students.set(list);
      // an existing lesson whose price does not follow the rate was priced by hand — keep it
      const existing = this.data.lesson?.price;
      const auto = this.autoPrice();
      if (existing != null && auto != null && existing !== auto) {
        this.form.controls.customPrice.setValue(true);
      }
    });

    this.form.controls.studentId.valueChanges.subscribe((v) => this.selectedStudentId.set(v ?? ''));
    this.form.controls.durationMinutes.valueChanges.subscribe((v) =>
      this.durationMinutes.set(v ?? 60),
    );
    this.form.controls.customPrice.valueChanges.subscribe((v) => this.customPrice.set(!!v));

    // as long as the tutor has not overridden it, the price follows rate × duration
    effect(() => {
      const auto = this.autoPrice();
      const price = this.form.controls.price;
      if (this.customPrice() || auto == null) {
        price.enable({ emitEvent: false });
        return;
      }
      price.setValue(auto, { emitEvent: false });
      price.disable({ emitEvent: false });
    });
  }

  /** 1 h / 1.5 h / 2 h, plus the length this lesson already has when it is a different one. */
  readonly durations = computed(() => {
    const minutes = new Set([...LessonDialogComponent.STANDARD_DURATIONS, this.durationMinutes()]);
    return [...minutes].sort((a, b) => a - b).map((m) => ({ minutes: m, label: durationLabel(m) }));
  });

  readonly rate = computed(
    () => this.students().find((s) => s.id === this.selectedStudentId())?.lessonPrice ?? null,
  );

  readonly selectedStudentName = computed(
    () => this.students().find((s) => s.id === this.selectedStudentId())?.fullName ?? 'student',
  );

  readonly factorLabel = computed(() => `${this.durationMinutes() / 60}`);

  /** Rate × duration factor, rounded to cents — the same rule the backend applies. */
  readonly autoPrice = computed(() => {
    const rate = this.rate();
    if (rate == null) {
      return null;
    }
    return Math.round(rate * (this.durationMinutes() / 60) * 100) / 100;
  });

  private initialDuration(): number {
    if (!this.data.lesson) {
      return 60;
    }
    const start = new Date(this.data.lesson.startTime).getTime();
    const end = new Date(this.data.lesson.endTime).getTime();
    return Math.max(15, Math.round((end - start) / 60000));
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
        // an untouched price is left to the backend so the rate stays the single source of truth
        price:
          (v.customPrice || this.autoPrice() == null) && v.price != null ? Number(v.price) : null,
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

function durationLabel(minutes: number): string {
  if (minutes % 60 === 0) {
    return `${minutes / 60} h`;
  }
  return minutes === 90 ? '1.5 h' : `${minutes} min`;
}

function roundToNextHour(): Date {
  const d = new Date();
  d.setMinutes(0, 0, 0);
  d.setHours(d.getHours() + 1);
  return d;
}
