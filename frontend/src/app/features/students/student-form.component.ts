import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { dayLabel } from './student.labels';
import {
  DAYS_OF_WEEK,
  LessonFormat,
  Student,
  StudentRequest,
  StudentStatus,
} from './student.model';

@Component({
  selector: 'app-student-form',
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './student-form.component.html',
  styleUrl: './student-form.component.scss',
})
export class StudentFormComponent {
  private readonly fb = inject(FormBuilder);

  readonly days = DAYS_OF_WEEK;
  readonly dayLabel = dayLabel;
  readonly formats: LessonFormat[] = ['ONLINE', 'OFFLINE', 'BOTH'];
  readonly statuses: StudentStatus[] = ['ACTIVE', 'PAUSED', 'FINISHED'];

  @Input() warnings: string[] = [];
  @Input() submitLabel = 'Save';
  @Input() busy = false;
  @Output() save = new EventEmitter<StudentRequest>();
  @Output() cancelled = new EventEmitter<void>();

  readonly form: FormGroup = this.fb.group({
    firstName: ['', [Validators.required, Validators.maxLength(255)]],
    lastName: ['', [Validators.required, Validators.maxLength(255)]],
    // only the name and the e-mail are required — everything else can be filled in later
    email: ['', [Validators.required, Validators.email]],
    phone: [''],
    isikukood: [''],
    age: [null as number | null],
    grade: [null as number | null],
    school: [''],
    subject: [''],
    goal: [''],
    level: [''],
    notes: [''],
    lessonFormat: [null as LessonFormat | null],
    lessonPrice: [null as number | null],
    status: ['ACTIVE' as StudentStatus],
    startDate: [null as string | null],
    endDate: [null as string | null],
    leaveReason: [''],
    parentName: [''],
    parentPhone: [''],
    parentEmail: ['', [Validators.email]],
    parentSecondaryPhone: [''],
    parentIsikukood: [''],
    telegram: [''],
    schedules: this.fb.array([]),
  });

  @Input() set value(v: Partial<StudentRequest> | Student | null | undefined) {
    if (!v) return;
    this.form.patchValue({
      ...v,
      status: v.status ?? 'ACTIVE',
    });
    this.schedules.clear();
    for (const s of v.schedules ?? []) {
      this.schedules.push(this.scheduleGroup(s));
    }
  }

  get schedules(): FormArray {
    return this.form.get('schedules') as FormArray;
  }

  addSchedule(): void {
    this.schedules.push(this.scheduleGroup());
  }

  removeSchedule(i: number): void {
    this.schedules.removeAt(i);
  }

  private scheduleGroup(s?: Partial<StudentRequest['schedules'][number]>): FormGroup {
    return this.fb.group({
      // an unfinished slot is dropped on submit rather than blocking the whole form
      dayOfWeek: [s?.dayOfWeek ?? null],
      startTime: [hhmm(s?.startTime)],
      endTime: [hhmm(s?.endTime)],
      lessonsPerWeek: [s?.lessonsPerWeek ?? null],
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    const request: StudentRequest = {
      ...raw,
      age: emptyToNull(raw.age),
      grade: emptyToNull(raw.grade),
      lessonPrice: emptyToNull(raw.lessonPrice),
      schedules: (raw.schedules ?? []).filter((s: { dayOfWeek: unknown }) => !!s.dayOfWeek),
    };
    this.save.emit(request);
  }
}

function emptyToNull(v: unknown): number | null {
  return v === '' || v === null || v === undefined ? null : Number(v);
}

/** API returns LocalTime as "HH:mm:ss"; the native time input wants "HH:mm". */
function hhmm(v: string | null | undefined): string | null {
  return v ? v.slice(0, 5) : null;
}
