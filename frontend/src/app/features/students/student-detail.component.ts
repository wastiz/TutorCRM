import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { firstValueFrom } from 'rxjs';
import { ConfirmDialogComponent } from '../../core/ui/confirm-dialog.component';
import { dayLabel, formatLabel, statusLabel } from './student.labels';
import { Student } from './student.model';
import { StudentService } from './student.service';

@Component({
  selector: 'app-student-detail',
  imports: [
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatDividerModule,
    MatProgressBarModule,
  ],
  template: `
    <div class="page">
      @if (loading()) {
        <mat-progress-bar mode="indeterminate" />
      } @else if (student(); as s) {
        <div class="page-header">
          <h1>#{{ s.studentNumber }} · {{ s.firstName }} {{ s.lastName }}</h1>
          <div class="header-actions">
            <a mat-stroked-button [routerLink]="['/students', s.id, 'edit']">
              <mat-icon>edit</mat-icon> Edit
            </a>
            @if (s.status !== 'FINISHED') {
              <button mat-stroked-button (click)="archive(s)"><mat-icon>archive</mat-icon> Archive</button>
            }
            <button mat-stroked-button color="warn" (click)="remove(s)">
              <mat-icon>delete</mat-icon> Delete
            </button>
          </div>
        </div>

        <div class="cards">
          <mat-card>
            <mat-card-header><mat-card-title>Student information</mat-card-title></mat-card-header>
            <mat-card-content>
              <dl>
                <dt>Status</dt><dd>{{ statusLabel(s.status) }}</dd>
                <dt>Email</dt><dd>{{ s.email || '—' }}</dd>
                <dt>Phone</dt><dd>{{ s.phone || '—' }}</dd>
                <dt>Isikukood</dt><dd>{{ s.isikukood || '—' }}</dd>
                <dt>Age</dt><dd>{{ s.age ?? '—' }}</dd>
                <dt>Grade</dt><dd>{{ s.grade ?? '—' }}</dd>
                <dt>School</dt><dd>{{ s.school || '—' }}</dd>
                <dt>Subject</dt><dd>{{ s.subject || '—' }}</dd>
                <dt>Level</dt><dd>{{ s.level || '—' }}</dd>
                <dt>Format</dt><dd>{{ formatLabel(s.lessonFormat) }}</dd>
                <dt>Lesson price</dt><dd>{{ s.lessonPrice != null ? ('€' + s.lessonPrice) : '—' }}</dd>
                <dt>Goal</dt><dd>{{ s.goal || '—' }}</dd>
                <dt>Notes</dt><dd>{{ s.notes || '—' }}</dd>
              </dl>
            </mat-card-content>
          </mat-card>

          <mat-card>
            <mat-card-header><mat-card-title>Schedule</mat-card-title></mat-card-header>
            <mat-card-content>
              @if (s.schedules.length) {
                <ul class="plain">
                  @for (row of s.schedules; track $index) {
                    <li>
                      {{ dayLabel(row.dayOfWeek) }}
                      @if (row.startTime) { · {{ row.startTime }}–{{ row.endTime }} }
                      @if (row.lessonsPerWeek) { · {{ row.lessonsPerWeek }}/week }
                    </li>
                  }
                </ul>
              } @else {
                <p class="muted">No preferred schedule set.</p>
              }
            </mat-card-content>
          </mat-card>

          <mat-card>
            <mat-card-header><mat-card-title>Parent information</mat-card-title></mat-card-header>
            <mat-card-content>
              <dl>
                <dt>Name</dt><dd>{{ s.parentName || '—' }}</dd>
                <dt>Phone</dt><dd>{{ s.parentPhone || '—' }}</dd>
                <dt>Secondary phone</dt><dd>{{ s.parentSecondaryPhone || '—' }}</dd>
                <dt>Email</dt><dd>{{ s.parentEmail || '—' }}</dd>
              </dl>
            </mat-card-content>
          </mat-card>

          <mat-card>
            <mat-card-header><mat-card-title>Lessons</mat-card-title></mat-card-header>
            <mat-card-content>
              <p class="muted">Upcoming and past lessons appear here once the Lessons module is enabled (Phase 4).</p>
            </mat-card-content>
          </mat-card>
        </div>
      }
    </div>
  `,
  styles: [
    `
      .header-actions {
        display: flex;
        gap: 8px;
      }
      .cards {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
        gap: 16px;
      }
      dl {
        display: grid;
        grid-template-columns: 130px 1fr;
        row-gap: 6px;
        margin: 0;
      }
      dt {
        color: rgba(0, 0, 0, 0.55);
      }
      ul.plain {
        margin: 0;
        padding-left: 18px;
      }
    `,
  ],
})
export class StudentDetailComponent implements OnInit {
  private readonly service = inject(StudentService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);

  readonly student = signal<Student | null>(null);
  readonly loading = signal(true);

  readonly statusLabel = statusLabel;
  readonly formatLabel = formatLabel;
  readonly dayLabel = dayLabel;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.service.get(id).subscribe({
      next: (s) => {
        this.student.set(s);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  async archive(s: Student): Promise<void> {
    const ok = await this.confirm(`Archive ${s.firstName} ${s.lastName}?`, 'They will be marked as finished.');
    if (!ok) return;
    const updated = await firstValueFrom(this.service.archive(s.id));
    this.student.set(updated);
  }

  async remove(s: Student): Promise<void> {
    const ok = await this.confirm(`Delete ${s.firstName} ${s.lastName}?`, 'This cannot be undone.');
    if (!ok) return;
    await firstValueFrom(this.service.delete(s.id));
    await this.router.navigate(['/students']);
  }

  private confirm(title: string, message: string): Promise<boolean> {
    return firstValueFrom(
      this.dialog
        .open(ConfirmDialogComponent, { data: { title, message }, width: '420px' })
        .afterClosed(),
    ).then((v) => v === true);
  }
}
