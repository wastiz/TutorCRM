import { DecimalPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { ConfirmDialogComponent } from '../../core/ui/confirm-dialog.component';
import { MonthlyReport } from './report.model';
import { ExportResult, ReportService } from './report.service';

@Component({
  selector: 'app-reports',
  imports: [
    ReactiveFormsModule,
    DecimalPipe,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatProgressBarModule,
  ],
  templateUrl: './reports.component.html',
  styleUrl: './reports.component.scss',
})
export class ReportsComponent implements OnInit {
  private readonly service = inject(ReportService);
  private readonly snack = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);

  readonly columns = [
    'fullName',
    'studentNumber',
    'email',
    'parentName',
    'isikukood',
    'subject',
    'lessonCount',
    'lessonPrice',
    'total',
    'grandTotal',
  ];

  readonly month = new FormControl<string>(currentMonth(), { nonNullable: true });
  readonly report = signal<MonthlyReport | null>(null);
  readonly loading = signal(false);
  readonly exporting = signal(false);

  ngOnInit(): void {
    this.month.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.load());
    this.load();
  }

  private load(): void {
    const m = this.month.value;
    if (!m) return;
    this.loading.set(true);
    this.service.monthly(m).subscribe({
      next: (r) => {
        this.report.set(r);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  async exportToSheets(r: MonthlyReport): Promise<void> {
    this.exporting.set(true);
    try {
      let result: ExportResult;
      try {
        result = await firstValueFrom(this.service.export(r.month, false));
      } catch (err) {
        if (
          err instanceof HttpErrorResponse &&
          err.status === 409 &&
          err.error?.code === 'WORKSHEET_EXISTS'
        ) {
          const title = err.error?.details?.worksheetTitle ?? r.title;
          const ok = await firstValueFrom(
            this.dialog
              .open(ConfirmDialogComponent, {
                data: {
                  title: `Worksheet "${title}" already exists`,
                  message: 'Overwrite it with the current figures?',
                  confirmLabel: 'Update existing',
                },
                width: '440px',
              })
              .afterClosed(),
          );
          if (ok !== true) return;
          result = await firstValueFrom(this.service.export(r.month, true));
        } else {
          return; // already surfaced by the global error interceptor
        }
      }
      this.snack
        .open(
          `Exported to "${result.worksheetTitle}"` +
            (result.updated ? ' (updated existing sheet)' : ''),
          'Open',
          { duration: 8000 },
        )
        .onAction()
        .subscribe(() => window.open(result.spreadsheetUrl, '_blank'));
    } finally {
      this.exporting.set(false);
    }
  }
}

function currentMonth(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
}
