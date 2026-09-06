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
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { debounceTime } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { formatLabel, statusLabel, telegramUrl } from './student.labels';
import { StudentStatus, StudentSummary } from './student.model';
import { StudentService } from './student.service';

@Component({
  selector: 'app-students-list',
  imports: [
    RouterLink,
    DatePipe,
    ReactiveFormsModule,
    MatTableModule,
    MatTooltipModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressBarModule,
  ],
  templateUrl: './students-list.component.html',
  styleUrl: './students-list.component.scss',
})
export class StudentsListComponent implements OnInit {
  private readonly service = inject(StudentService);
  private readonly destroyRef = inject(DestroyRef);

  readonly telegramUrl = telegramUrl;

  readonly columns = [
    'number',
    'name',
    'subject',
    'grade',
    'format',
    'price',
    'telegram',
    'status',
    'next',
  ];
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
