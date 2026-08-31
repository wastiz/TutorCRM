import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { debounceTime } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { formatLabel, statusLabel } from './student.labels';
import { StudentStatus, StudentSummary } from './student.model';
import { StudentService } from './student.service';

@Component({
  selector: 'app-students-list',
  imports: [
    RouterLink,
    DatePipe,
    ReactiveFormsModule,
    MatTableModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressBarModule,
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <h1>Students</h1>
        <a mat-flat-button color="primary" routerLink="/students/new">
          <mat-icon>add</mat-icon>
          Create student
        </a>
      </div>

      <div class="filters">
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Search</mat-label>
          <input matInput [formControl]="search" placeholder="Name, email, phone, subject…" />
          <mat-icon matSuffix>search</mat-icon>
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Status</mat-label>
          <mat-select [formControl]="status">
            <mat-option [value]="null">All</mat-option>
            <mat-option value="ACTIVE">Active</mat-option>
            <mat-option value="PAUSED">Paused</mat-option>
            <mat-option value="FINISHED">Finished</mat-option>
          </mat-select>
        </mat-form-field>
      </div>

      @if (loading()) {
        <mat-progress-bar mode="indeterminate" />
      }

      <table mat-table [dataSource]="students()" class="full-width">
        <ng-container matColumnDef="number">
          <th mat-header-cell *matHeaderCellDef>#</th>
          <td mat-cell *matCellDef="let s">{{ s.studentNumber }}</td>
        </ng-container>
        <ng-container matColumnDef="name">
          <th mat-header-cell *matHeaderCellDef>Name</th>
          <td mat-cell *matCellDef="let s">
            <a [routerLink]="['/students', s.id]">{{ s.fullName }}</a>
          </td>
        </ng-container>
        <ng-container matColumnDef="subject">
          <th mat-header-cell *matHeaderCellDef>Subject</th>
          <td mat-cell *matCellDef="let s">{{ s.subject || '—' }}</td>
        </ng-container>
        <ng-container matColumnDef="grade">
          <th mat-header-cell *matHeaderCellDef>Grade</th>
          <td mat-cell *matCellDef="let s">{{ s.grade ?? '—' }}</td>
        </ng-container>
        <ng-container matColumnDef="format">
          <th mat-header-cell *matHeaderCellDef>Format</th>
          <td mat-cell *matCellDef="let s">{{ formatLabel(s.lessonFormat) }}</td>
        </ng-container>
        <ng-container matColumnDef="price">
          <th mat-header-cell *matHeaderCellDef>Price</th>
          <td mat-cell *matCellDef="let s">{{ s.lessonPrice != null ? ('€' + s.lessonPrice) : '—' }}</td>
        </ng-container>
        <ng-container matColumnDef="status">
          <th mat-header-cell *matHeaderCellDef>Status</th>
          <td mat-cell *matCellDef="let s">
            <span class="badge" [class]="'badge-' + s.status.toLowerCase()">{{ statusLabel(s.status) }}</span>
          </td>
        </ng-container>
        <ng-container matColumnDef="next">
          <th mat-header-cell *matHeaderCellDef>Next lesson</th>
          <td mat-cell *matCellDef="let s">{{ s.nextLessonAt ? (s.nextLessonAt | date: 'short') : '—' }}</td>
        </ng-container>

        <tr mat-header-row *matHeaderRowDef="columns"></tr>
        <tr mat-row *matRowDef="let row; columns: columns"></tr>
      </table>

      @if (!loading() && students().length === 0) {
        <p class="muted empty">No students yet. Create one or import from a message.</p>
      }
    </div>
  `,
  styles: [
    `
      .filters {
        display: flex;
        gap: 16px;
        margin-bottom: 12px;
        flex-wrap: wrap;
      }
      .filters mat-form-field {
        min-width: 260px;
      }
      table {
        background: #fff;
      }
      .empty {
        margin-top: 32px;
        text-align: center;
      }
      .badge {
        padding: 2px 10px;
        border-radius: 12px;
        font-size: 12px;
        font-weight: 500;
      }
      .badge-active {
        background: #e8f5e9;
        color: #2e7d32;
      }
      .badge-paused {
        background: #fff8e1;
        color: #f9a825;
      }
      .badge-finished {
        background: #eceff1;
        color: #546e7a;
      }
    `,
  ],
})
export class StudentsListComponent implements OnInit {
  private readonly service = inject(StudentService);
  private readonly destroyRef = inject(DestroyRef);

  readonly columns = ['number', 'name', 'subject', 'grade', 'format', 'price', 'status', 'next'];
  readonly formatLabel = formatLabel;
  readonly statusLabel = statusLabel;

  readonly students = signal<StudentSummary[]>([]);
  readonly loading = signal(false);

  readonly search = new FormControl<string>('', { nonNullable: true });
  readonly status = new FormControl<StudentStatus | null>(null);

  ngOnInit(): void {
    this.search.valueChanges
      .pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.reload());
    this.status.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.reload());
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.service.list(this.search.value, this.status.value).subscribe({
      next: (list) => {
        this.students.set(list);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
