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
  templateUrl: './google-import-dialog.component.html',
  styleUrl: './google-import-dialog.component.scss',
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
    () =>
      this.selectedCount() > 0 || (this.updateMoved && this.data.preview.movedLessons.length > 0),
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
