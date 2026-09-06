import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { firstValueFrom } from 'rxjs';
import { StudentFormComponent } from './student-form.component';
import { Student, StudentRequest } from './student.model';
import { StudentService } from './student.service';

@Component({
  selector: 'app-student-edit',
  imports: [StudentFormComponent, MatProgressBarModule],
  templateUrl: './student-edit.component.html',
})
export class StudentEditComponent implements OnInit {
  private readonly service = inject(StudentService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly student = signal<Student | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);

  private get id(): string {
    return this.route.snapshot.paramMap.get('id')!;
  }

  ngOnInit(): void {
    this.service.get(this.id).subscribe({
      next: (s) => {
        this.student.set(s);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  async save(request: StudentRequest): Promise<void> {
    this.busy.set(true);
    try {
      await firstValueFrom(this.service.update(this.id, request));
      await this.router.navigate(['/students', this.id]);
    } finally {
      this.busy.set(false);
    }
  }

  cancel(): void {
    void this.router.navigate(['/students', this.id]);
  }
}
