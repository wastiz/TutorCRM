import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { firstValueFrom } from 'rxjs';
import {
  EMAIL_TEMPLATE_KINDS,
  EMAIL_TEMPLATE_KIND_LABEL,
  EmailPlaceholder,
  EmailTemplate,
  EmailTemplateKind,
} from './email.model';
import { EmailService } from './email.service';

export interface EmailTemplateDialogData {
  template?: EmailTemplate;
}

@Component({
  selector: 'app-email-template-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatTooltipModule,
  ],
  templateUrl: './email-template-dialog.component.html',
  styleUrl: './email-template-dialog.component.scss',
})
export class EmailTemplateDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(EmailService);
  private readonly ref = inject(MatDialogRef<EmailTemplateDialogComponent, EmailTemplate>);
  readonly data = inject<EmailTemplateDialogData>(MAT_DIALOG_DATA);

  readonly kinds = EMAIL_TEMPLATE_KINDS;
  readonly kindLabel = (k: EmailTemplateKind) => EMAIL_TEMPLATE_KIND_LABEL[k];
  readonly placeholders = signal<EmailPlaceholder[]>([]);
  readonly busy = signal(false);

  readonly form = this.fb.group({
    name: [this.data.template?.name ?? '', [Validators.required, Validators.maxLength(255)]],
    kind: [this.data.template?.kind ?? ('GENERAL' as EmailTemplateKind)],
    subject: [this.data.template?.subject ?? '', [Validators.required, Validators.maxLength(500)]],
    body: [this.data.template?.body ?? '', Validators.required],
  });

  constructor() {
    this.service.placeholders().subscribe((p) => this.placeholders.set(p));
  }

  /** Appends a placeholder to the body — quicker and safer than typing the braces. */
  insert(name: string): void {
    const body = this.form.controls.body;
    body.setValue(`${body.value ?? ''}{{${name}}}`);
  }

  async save(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.busy.set(true);
    try {
      const v = this.form.getRawValue();
      const request = {
        name: v.name!,
        kind: v.kind as EmailTemplateKind,
        subject: v.subject!,
        body: v.body!,
      };
      const saved = this.data.template
        ? await firstValueFrom(this.service.updateTemplate(this.data.template.id, request))
        : await firstValueFrom(this.service.createTemplate(request));
      this.ref.close(saved);
    } finally {
      this.busy.set(false);
    }
  }
}
