import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatListModule } from '@angular/material/list';
import { StudentSummary } from './student.model';

export type DuplicateChoice = { action: 'anyway' } | { action: 'use'; student: StudentSummary };

@Component({
  selector: 'app-duplicate-dialog',
  imports: [MatDialogModule, MatButtonModule, MatListModule],
  template: `
    <h2 mat-dialog-title>Possible duplicate found</h2>
    <mat-dialog-content>
      <p class="muted">A student with matching details already exists:</p>
      <mat-list>
        @for (s of data.duplicates; track s.id) {
          <mat-list-item>
            <span matListItemTitle>#{{ s.studentNumber }} · {{ s.fullName }}</span>
            <span matListItemLine>{{ s.email || s.phone || '—' }}</span>
            <button mat-button color="primary" (click)="use(s)">Use this student</button>
          </mat-list-item>
        }
      </mat-list>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button color="primary" (click)="createAnyway()">Create anyway</button>
    </mat-dialog-actions>
  `,
})
export class DuplicateDialogComponent {
  readonly data = inject<{ duplicates: StudentSummary[] }>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<DuplicateDialogComponent, DuplicateChoice>);

  use(student: StudentSummary): void {
    this.ref.close({ action: 'use', student });
  }

  createAnyway(): void {
    this.ref.close({ action: 'anyway' });
  }
}
