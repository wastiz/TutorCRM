import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';

export interface ConfirmData {
  title: string;
  message?: string;
  confirmLabel?: string;
  confirmColor?: 'primary' | 'warn';
}

@Component({
  selector: 'app-confirm-dialog',
  imports: [MatDialogModule, MatButtonModule],
  templateUrl: './confirm-dialog.component.html',
})
export class ConfirmDialogComponent {
  readonly data = inject<ConfirmData>(MAT_DIALOG_DATA);
}
