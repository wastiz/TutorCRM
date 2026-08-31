import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import {
  FormArray,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { dayLabel } from './student.labels';
import { DAYS_OF_WEEK, LessonFormat, Student, StudentRequest, StudentStatus } from './student.model';

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
  template: `
    @if (warnings.length) {
      <div class="warnings">
        <mat-icon>warning</mat-icon>
        <ul>
          @for (w of warnings; track w) {
            <li>{{ w }}</li>
          }
        </ul>
      </div>
    }

    <form [formGroup]="form" (ngSubmit)="onSubmit()">
      <div class="grid">
        <mat-form-field>
          <mat-label>First name</mat-label>
          <input matInput formControlName="firstName" required />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Last name</mat-label>
          <input matInput formControlName="lastName" required />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Email</mat-label>
          <input matInput type="email" formControlName="email" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Phone</mat-label>
          <input matInput formControlName="phone" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Isikukood</mat-label>
          <input matInput formControlName="isikukood" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Age</mat-label>
          <input matInput type="number" formControlName="age" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Grade</mat-label>
          <input matInput type="number" formControlName="grade" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>School</mat-label>
          <input matInput formControlName="school" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Subject</mat-label>
          <input matInput formControlName="subject" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Level</mat-label>
          <input matInput formControlName="level" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Lesson format</mat-label>
          <mat-select formControlName="lessonFormat">
            <mat-option [value]="null">—</mat-option>
            @for (f of formats; track f) {
              <mat-option [value]="f">{{ f }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Lesson price (€)</mat-label>
          <input matInput type="number" step="0.01" formControlName="lessonPrice" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Status</mat-label>
          <mat-select formControlName="status">
            @for (s of statuses; track s) {
              <mat-option [value]="s">{{ s }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Start date</mat-label>
          <input matInput type="date" formControlName="startDate" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>End date</mat-label>
          <input matInput type="date" formControlName="endDate" />
        </mat-form-field>
      </div>

      <mat-form-field class="wide">
        <mat-label>Goal / description</mat-label>
        <textarea matInput rows="2" formControlName="goal"></textarea>
      </mat-form-field>
      <mat-form-field class="wide">
        <mat-label>Notes</mat-label>
        <textarea matInput rows="2" formControlName="notes"></textarea>
      </mat-form-field>

      <h3>Parent</h3>
      <div class="grid">
        <mat-form-field>
          <mat-label>Parent name</mat-label>
          <input matInput formControlName="parentName" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Parent phone</mat-label>
          <input matInput formControlName="parentPhone" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Parent email</mat-label>
          <input matInput type="email" formControlName="parentEmail" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Parent secondary phone</mat-label>
          <input matInput formControlName="parentSecondaryPhone" />
        </mat-form-field>
      </div>

      <h3>
        Preferred schedule
        <button mat-icon-button type="button" (click)="addSchedule()" aria-label="Add slot">
          <mat-icon>add</mat-icon>
        </button>
      </h3>
      <div formArrayName="schedules">
        @for (row of schedules.controls; track row; let i = $index) {
          <div class="schedule-row" [formGroupName]="i">
            <mat-form-field>
              <mat-label>Day</mat-label>
              <mat-select formControlName="dayOfWeek">
                @for (d of days; track d) {
                  <mat-option [value]="d">{{ dayLabel(d) }}</mat-option>
                }
              </mat-select>
            </mat-form-field>
            <mat-form-field>
              <mat-label>From</mat-label>
              <input matInput type="time" formControlName="startTime" />
            </mat-form-field>
            <mat-form-field>
              <mat-label>To</mat-label>
              <input matInput type="time" formControlName="endTime" />
            </mat-form-field>
            <mat-form-field>
              <mat-label>Lessons / week</mat-label>
              <input matInput type="number" formControlName="lessonsPerWeek" />
            </mat-form-field>
            <button mat-icon-button type="button" (click)="removeSchedule(i)" aria-label="Remove slot">
              <mat-icon>delete</mat-icon>
            </button>
          </div>
        }
      </div>

      <div class="actions">
        <button mat-button type="button" (click)="cancelled.emit()">Cancel</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="busy || form.invalid">
          {{ submitLabel }}
        </button>
      </div>
    </form>
  `,
  styles: [
    `
      .grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
        gap: 8px 16px;
      }
      .wide {
        width: 100%;
      }
      mat-form-field {
        width: 100%;
      }
      h3 {
        margin: 20px 0 8px;
        display: flex;
        align-items: center;
        gap: 8px;
      }
      .schedule-row {
        display: grid;
        grid-template-columns: repeat(4, 1fr) auto;
        gap: 12px;
        align-items: center;
      }
      .actions {
        display: flex;
        justify-content: flex-end;
        gap: 12px;
        margin-top: 24px;
      }
      .warnings {
        display: flex;
        gap: 8px;
        background: #fff3e0;
        border: 1px solid #ffcc80;
        border-radius: 6px;
        padding: 8px 12px;
        margin-bottom: 16px;
      }
      .warnings ul {
        margin: 0;
        padding-left: 18px;
      }
    `,
  ],
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
    email: ['', [Validators.email]],
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
    parentName: [''],
    parentPhone: [''],
    parentEmail: ['', [Validators.email]],
    parentSecondaryPhone: [''],
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
      dayOfWeek: [s?.dayOfWeek ?? null, Validators.required],
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
