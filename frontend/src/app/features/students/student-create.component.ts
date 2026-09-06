import { Component, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatTabsModule } from '@angular/material/tabs';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { DuplicateChoice, DuplicateDialogComponent } from './duplicate-dialog.component';
import { StudentFormComponent } from './student-form.component';
import { StudentRequest } from './student.model';
import { StudentService } from './student.service';

@Component({
  selector: 'app-student-create',
  imports: [
    ReactiveFormsModule,
    MatTabsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    StudentFormComponent,
  ],
  templateUrl: './student-create.component.html',
  styleUrl: './student-create.component.scss',
})
export class StudentCreateComponent {
  private readonly service = inject(StudentService);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);

  readonly rawText = new FormControl<string>('', { nonNullable: true });
  readonly parsing = signal(false);
  readonly busy = signal(false);
  readonly parsed = signal<import('./student.model').StudentImportPreview | null>(null);

  parse(): void {
    this.parsing.set(true);
    this.service.parseImport(this.rawText.value).subscribe({
      next: (preview) => {
        this.parsed.set(preview);
        this.parsing.set(false);
      },
      error: () => this.parsing.set(false),
    });
  }

  resetImport(): void {
    this.parsed.set(null);
  }

  async create(request: StudentRequest): Promise<void> {
    this.busy.set(true);
    try {
      const { duplicates } = await firstValueFrom(
        this.service.checkDuplicates({
          email: request.email,
          phone: request.phone,
          firstName: request.firstName,
          lastName: request.lastName,
        }),
      );

      let payload = request;
      if (duplicates.length > 0) {
        const choice = await firstValueFrom(
          this.dialog
            .open<DuplicateDialogComponent, unknown, DuplicateChoice>(DuplicateDialogComponent, {
              data: { duplicates },
              width: '480px',
            })
            .afterClosed(),
        );
        if (!choice) {
          this.busy.set(false);
          return;
        }
        if (choice.action === 'use') {
          await this.router.navigate(['/students', choice.student.id]);
          return;
        }
        payload = { ...request, ignoreDuplicates: true };
      }

      const created = await firstValueFrom(this.service.create(payload));
      await this.router.navigate(['/students', created.id]);
    } finally {
      this.busy.set(false);
    }
  }

  cancel(): void {
    void this.router.navigate(['/students']);
  }
}
