import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatListModule } from '@angular/material/list';
import { StudentSummary } from './student.model';

export type DuplicateChoice = { action: 'anyway' } | { action: 'use'; student: StudentSummary };

@Component({
  selector: 'app-duplicate-dialog',
  imports: [MatDialogModule, MatButtonModule, MatListModule],
  templateUrl: './duplicate-dialog.component.html',
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
