import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { firstValueFrom } from 'rxjs';
import { StudentSummary } from '../students/student.model';
import { StudentService } from '../students/student.service';
import { GoogleImportPreview, GoogleImportResult } from './lesson.model';
import { LessonService } from './lesson.service';

interface Row {
  eventId: string;
  summary: string | null;
  start: string;
  end: string;
  include: boolean;
  studentId: string | null;
  price: number | null;
  guessed: boolean;
}

@Component({
  selector: 'app-google-import-dialog',
  imports: [
    FormsModule,
    DatePipe,
    MatDialogModule,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatIconModule,
    MatTooltipModule,
  ],
  template: `
    <h2 mat-dialog-title>Sync from Google Calendar</h2>
    <mat-dialog-content>
      @if (!data.preview.enabled) {
        <p class="muted">Connect Google and choose a calendar in Settings first.</p>
      } @else {
        <p class="muted">
          {{ data.preview.from | date: 'd MMM' }} – {{ data.preview.to | date: 'd MMM y' }} ·
          {{ data.preview.alreadyLinked }} event(s) already linked
        </p>

        @if (rows().length) {
          <h3>New events → lessons</h3>
          <table class="rows">
            <tr>
              <th></th><th>Event</th><th>When</th><th>Student</th><th>€</th>
            </tr>
            @for (r of rows(); track r.eventId) {
              <tr [class.off]="!r.include">
                <td><mat-checkbox [(ngModel)]="r.include"></mat-checkbox></td>
                <td class="summary">{{ r.summary || '(no title)' }}</td>
                <td class="when">
                  {{ r.start | date: 'EEE d MMM, HH:mm' }}–{{ r.end | date: 'HH:mm' }}
                </td>
                <td>
                  <mat-form-field appearance="outline" subscriptSizing="dynamic">
                    <mat-select [(ngModel)]="r.studentId" placeholder="pick student">
                      @for (s of students(); track s.id) {
                        <mat-option [value]="s.id">#{{ s.studentNumber }} {{ s.fullName }}</mat-option>
                      }
                    </mat-select>
                  </mat-form-field>
                  @if (r.guessed) { <mat-icon class="guess" matTooltip="matched from the title">auto_awesome</mat-icon> }
                </td>
                <td>
                  <mat-form-field appearance="outline" subscriptSizing="dynamic" class="price">
                    <input matInput type="number" step="0.01" [(ngModel)]="r.price" />
                  </mat-form-field>
                </td>
              </tr>
            }
          </table>
        } @else {
          <p class="muted">No new events to import in this range.</p>
        }

        @if (data.preview.movedLessons.length) {
          <h3>Moved in Google</h3>
          <mat-checkbox [(ngModel)]="updateMoved">
            Update {{ data.preview.movedLessons.length }} lesson(s) whose time changed in Google Calendar
          </mat-checkbox>
          <ul class="moved">
            @for (m of data.preview.movedLessons; track m.eventId) {
              <li>
                {{ m.studentName }}:
                {{ m.currentStart | date: 'd MMM HH:mm' }} → <strong>{{ m.newStart | date: 'd MMM HH:mm' }}</strong>
              </li>
            }
          </ul>
        }
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button color="primary" [disabled]="!canImport() || busy()" (click)="run()">
        Import {{ selectedCount() }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      mat-dialog-content { min-width: 640px; }
      h3 { margin: 16px 0 6px; }
      table.rows { width: 100%; border-collapse: collapse; }
      table.rows th { text-align: left; font-size: 12px; color: rgba(0,0,0,0.5); padding: 4px; }
      table.rows td { padding: 4px; vertical-align: middle; }
      tr.off { opacity: 0.45; }
      .summary { max-width: 180px; }
      .when { white-space: nowrap; font-size: 12px; }
      .price { width: 80px; }
      .guess { color: #7b1fa2; font-size: 18px; vertical-align: middle; }
      ul.moved { margin: 6px 0 0; padding-left: 18px; font-size: 13px; }
    `,
  ],
})
export class GoogleImportDialogComponent {
  private readonly lessons = inject(LessonService);
  private readonly studentService = inject(StudentService);
  private readonly ref = inject(MatDialogRef<GoogleImportDialogComponent, GoogleImportResult>);
  readonly data = inject<{ preview: GoogleImportPreview }>(MAT_DIALOG_DATA);

  readonly students = signal<StudentSummary[]>([]);
  readonly busy = signal(false);
  updateMoved = this.data.preview.movedLessons.length > 0;

  readonly rows = signal<Row[]>(
    this.data.preview.newEvents.map((e) => ({
      eventId: e.eventId,
      summary: e.summary,
      start: e.start,
      end: e.end,
      include: !!e.suggestedStudentId,
      studentId: e.suggestedStudentId,
      price: e.suggestedPrice,
      guessed: !!e.suggestedStudentId,
    })),
  );

  readonly selectedCount = computed(
    () => this.rows().filter((r) => r.include && r.studentId).length,
  );
  readonly canImport = computed(
    () => this.selectedCount() > 0 || (this.updateMoved && this.data.preview.movedLessons.length > 0),
  );

  constructor() {
    this.studentService.list().subscribe((s) => this.students.set(s));
  }

  async run(): Promise<void> {
    this.busy.set(true);
    try {
      const result = await firstValueFrom(
        this.lessons.googleImport({
          from: this.data.preview.from,
          to: this.data.preview.to,
          updateMoved: this.updateMoved,
          items: this.rows()
            .filter((r) => r.include && r.studentId)
            .map((r) => ({ eventId: r.eventId, studentId: r.studentId as string, price: r.price })),
        }),
      );
      this.ref.close(result);
    } finally {
      this.busy.set(false);
    }
  }
}
