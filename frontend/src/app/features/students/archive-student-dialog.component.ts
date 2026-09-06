import { Component, inject } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

export interface ArchiveStudentDialogData {
  studentName: string;
  reason?: string | null;
}

export interface ArchiveStudentDialogResult {
  reason: string;
}

/** Archiving is the moment the tutor knows why a student stopped — so it is asked for here. */
@Component({
  selector: 'app-archive-student-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
  ],
  templateUrl: './archive-student-dialog.component.html',
  styleUrl: './archive-student-dialog.component.scss',
})
export class ArchiveStudentDialogComponent {
  readonly data = inject<ArchiveStudentDialogData>(MAT_DIALOG_DATA);
  readonly reason = new FormControl<string>(this.data.reason ?? '', { nonNullable: true });
}
