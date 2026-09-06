import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { firstValueFrom } from 'rxjs';
import { EmailSendResult, EmailTemplate } from './email.model';
import { EmailService } from './email.service';

export interface SendEmailDialogData {
  studentId: string;
  studentName?: string;
  /** Pre-fills the {{lesson.*}} placeholders when the mail is about one lesson. */
  lessonId?: string;
  defaultRecipient?: string | null;
}

/**
 * Pick a template, see exactly what will be sent (placeholders already filled by the backend),
 * correct it if needed, then send. Nothing leaves the mailbox without this preview.
 */
@Component({
  selector: 'app-send-email-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './send-email-dialog.component.html',
  styleUrl: './send-email-dialog.component.scss',
})
export class SendEmailDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(EmailService);
  private readonly ref = inject(MatDialogRef<SendEmailDialogComponent, EmailSendResult>);
  readonly data = inject<SendEmailDialogData>(MAT_DIALOG_DATA);

  readonly templates = signal<EmailTemplate[]>([]);
  readonly warnings = signal<string[]>([]);
  readonly busy = signal(false);

  readonly form = this.fb.group({
    templateId: [null as string | null],
    to: [this.data.defaultRecipient ?? ''],
    subject: [''],
    body: [''],
  });

  constructor() {
    this.service.templates().subscribe((list) => this.templates.set(list));
    this.form.controls.templateId.valueChanges.subscribe((id) => void this.applyTemplate(id));
  }

  defaultRecipient(): string {
    return this.data.defaultRecipient ?? 'student or parent email';
  }

  /** Renders the chosen template on the server so the preview matches what is sent. */
  private async applyTemplate(templateId: string | null): Promise<void> {
    if (!templateId) {
      this.warnings.set([]);
      return;
    }
    this.busy.set(true);
    try {
      const preview = await firstValueFrom(
        this.service.preview({
          templateId,
          studentId: this.data.studentId,
          lessonId: this.data.lessonId ?? null,
          to: this.form.controls.to.value || null,
        }),
      );
      this.form.patchValue(
        { subject: preview.subject, body: preview.body, to: preview.to ?? '' },
        { emitEvent: false },
      );
      this.warnings.set([
        ...preview.warnings,
        ...(preview.unresolvedPlaceholders.length
          ? [`Still empty: ${preview.unresolvedPlaceholders.join(', ')}`]
          : []),
      ]);
    } finally {
      this.busy.set(false);
    }
  }

  async send(): Promise<void> {
    this.busy.set(true);
    try {
      const v = this.form.getRawValue();
      const result = await firstValueFrom(
        this.service.send({
          studentId: this.data.studentId,
          lessonId: this.data.lessonId ?? null,
          to: v.to || null,
          subject: v.subject || null,
          body: v.body || null,
        }),
      );
      this.ref.close(result);
    } finally {
      this.busy.set(false);
    }
  }
}
