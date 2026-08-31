import { DecimalPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { firstValueFrom } from 'rxjs';
import { MonthlyReport } from './report.model';
import { ReportService } from './report.service';

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
  template: `
    <div class="page">
      <div class="page-header">
        <h1>Reports</h1>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Month</mat-label>
          <input matInput type="month" [formControl]="month" />
        </mat-form-field>
      </div>

      @if (loading()) {
        <mat-progress-bar mode="indeterminate" />
      }

      @if (report(); as r) {
        <h2 class="title">{{ r.title }}</h2>

        @if (r.errors.length) {
          <div class="banner error">
            <mat-icon>error</mat-icon>
            <ul>@for (e of r.errors; track e) { <li>{{ e }}</li> }</ul>
          </div>
        }
        @if (r.warnings.length) {
          <div class="banner warn">
            <mat-icon>warning</mat-icon>
            <ul>@for (w of r.warnings; track w) { <li>{{ w }}</li> }</ul>
          </div>
        }

        <table mat-table [dataSource]="r.students" class="report">
          <ng-container matColumnDef="fullName">
            <th mat-header-cell *matHeaderCellDef>Имя фамилия латиницей</th>
            <td mat-cell *matCellDef="let s">{{ s.fullName }}</td>
            <td mat-footer-cell *matFooterCellDef>ИТОГО</td>
          </ng-container>
          <ng-container matColumnDef="studentNumber">
            <th mat-header-cell *matHeaderCellDef>Номер уч.</th>
            <td mat-cell *matCellDef="let s">{{ s.studentNumber }}</td>
            <td mat-footer-cell *matFooterCellDef></td>
          </ng-container>
          <ng-container matColumnDef="email">
            <th mat-header-cell *matHeaderCellDef>Почта</th>
            <td mat-cell *matCellDef="let s">{{ s.email }}</td>
            <td mat-footer-cell *matFooterCellDef></td>
          </ng-container>
          <ng-container matColumnDef="parentName">
            <th mat-header-cell *matHeaderCellDef>Имя родителя</th>
            <td mat-cell *matCellDef="let s">{{ s.parentName }}</td>
            <td mat-footer-cell *matFooterCellDef></td>
          </ng-container>
          <ng-container matColumnDef="isikukood">
            <th mat-header-cell *matHeaderCellDef>Isikukood</th>
            <td mat-cell *matCellDef="let s">{{ s.isikukood }}</td>
            <td mat-footer-cell *matFooterCellDef></td>
          </ng-container>
          <ng-container matColumnDef="subject">
            <th mat-header-cell *matHeaderCellDef>Предмет</th>
            <td mat-cell *matCellDef="let s">{{ s.subject }}</td>
            <td mat-footer-cell *matFooterCellDef></td>
          </ng-container>
          <ng-container matColumnDef="lessonCount">
            <th mat-header-cell *matHeaderCellDef class="num">Количество уроков</th>
            <td mat-cell *matCellDef="let s" class="num">{{ s.lessonCount }}</td>
            <td mat-footer-cell *matFooterCellDef class="num">{{ r.totalLessons }}</td>
          </ng-container>
          <ng-container matColumnDef="lessonPrice">
            <th mat-header-cell *matHeaderCellDef class="num">Цена урока</th>
            <td mat-cell *matCellDef="let s" class="num">
              {{ s.lessonPrice != null ? (s.lessonPrice | number: '1.0-2') : '⚠' }}
            </td>
            <td mat-footer-cell *matFooterCellDef></td>
          </ng-container>
          <ng-container matColumnDef="total">
            <th mat-header-cell *matHeaderCellDef class="num">Сумма</th>
            <td mat-cell *matCellDef="let s" class="num">{{ s.total | number: '1.0-2' }}</td>
            <td mat-footer-cell *matFooterCellDef></td>
          </ng-container>
          <ng-container matColumnDef="grandTotal">
            <th mat-header-cell *matHeaderCellDef class="num">Итого</th>
            <td mat-cell *matCellDef="let s" class="num"></td>
            <td mat-footer-cell *matFooterCellDef class="num">{{ r.totalAmount | number: '1.0-2' }}</td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="columns"></tr>
          <tr mat-row *matRowDef="let row; columns: columns"></tr>
          <tr mat-footer-row *matFooterRowDef="columns"></tr>
        </table>

        @if (!r.students.length) {
          <p class="muted empty">No completed lessons in {{ r.title }}.</p>
        }

        <div class="actions">
          <button
            mat-flat-button
            color="primary"
            [disabled]="r.errors.length > 0 || !r.students.length || exporting()"
            (click)="exportToSheets(r)"
          >
            <mat-icon>ios_share</mat-icon>
            Export to Google Sheets
          </button>
        </div>
      }
    </div>
  `,
  styles: [
    `
      .title { margin: 4px 0 12px; }
      table.report { width: 100%; background: #fff; }
      .num { text-align: right; }
      td.mat-mdc-footer-cell, th.mat-mdc-footer-cell { font-weight: 700; }
      .banner {
        display: flex; gap: 10px; padding: 10px 14px; border-radius: 6px; margin-bottom: 12px;
      }
      .banner ul { margin: 0; padding-left: 18px; }
      .banner.warn { background: #fff3e0; border: 1px solid #ffcc80; }
      .banner.error { background: #ffebee; border: 1px solid #ef9a9a; }
      .actions { margin-top: 20px; }
      .empty { margin-top: 24px; }
    `,
  ],
})
export class ReportsComponent implements OnInit {
  private readonly service = inject(ReportService);
  private readonly snack = inject(MatSnackBar);
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
      const result = await firstValueFrom(this.service.export(r.month));
      this.snack.open(
        `Exported to "${result.worksheetTitle}"` + (result.updated ? ' (updated existing sheet)' : ''),
        'Open',
        { duration: 8000 },
      ).onAction().subscribe(() => window.open(result.spreadsheetUrl, '_blank'));
    } finally {
      this.exporting.set(false);
    }
  }
}

function currentMonth(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
}
