import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatDialog } from '@angular/material/dialog';
import { MatListModule } from '@angular/material/list';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { ConfirmDialogComponent } from '../../core/ui/confirm-dialog.component';
import { EmailTemplateDialogComponent } from '../email/email-template-dialog.component';
import { EMAIL_TEMPLATE_KIND_LABEL, EmailTemplate, EmailTemplateKind } from '../email/email.model';
import { EmailService } from '../email/email.service';
import {
  GoogleCalendarSummary,
  GoogleStatus,
  Settings,
  SpreadsheetSummary,
  SpreadsheetWorksheet,
} from './settings.model';
import { SettingsService } from './settings.service';

@Component({
  selector: 'app-settings',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatProgressBarModule,
    MatListModule,
  ],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss',
})
export class SettingsComponent implements OnInit {
  private readonly service = inject(SettingsService);
  private readonly auth = inject(AuthService);
  private readonly snack = inject(MatSnackBar);
  private readonly emails = inject(EmailService);
  private readonly dialog = inject(MatDialog);

  readonly templates = signal<EmailTemplate[]>([]);
  readonly kindLabel = (k: EmailTemplateKind) => EMAIL_TEMPLATE_KIND_LABEL[k];

  readonly loading = signal(true);
  readonly status = signal<GoogleStatus | null>(null);
  readonly calendars = signal<GoogleCalendarSummary[]>([]);
  readonly spreadsheets = signal<SpreadsheetSummary[]>([]);
  readonly worksheets = signal<SpreadsheetWorksheet[]>([]);
  private readonly settings = signal<Settings | null>(null);

  readonly calendarId = new FormControl<string | null>(null);
  readonly spreadsheetId = new FormControl<string | null>(null);
  readonly worksheetTitle = new FormControl<string | null>(null);
  readonly perMonthSheet = new FormControl<boolean>(true, { nonNullable: true });

  readonly scopeList = computed(() =>
    (this.status()?.grantedScopes ?? [])
      .map((s) => s.replace('https://www.googleapis.com/auth/', ''))
      .join(', '),
  );

  /** Opens the editor for a new template, or for {@code template} when editing. */
  editTemplate(template?: EmailTemplate): void {
    this.dialog
      .open(EmailTemplateDialogComponent, { data: { template }, maxWidth: '90vw' })
      .afterClosed()
      .subscribe((saved) => {
        if (saved) this.loadTemplates();
      });
  }

  deleteTemplate(template: EmailTemplate): void {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: {
          title: 'Delete template',
          message: `Delete "${template.name}"?`,
          confirmLabel: 'Delete',
        },
      })
      .afterClosed()
      .subscribe(async (ok) => {
        if (!ok) return;
        await firstValueFrom(this.emails.deleteTemplate(template.id));
        this.loadTemplates();
      });
  }

  private loadTemplates(): void {
    this.emails.templates().subscribe((list) => this.templates.set(list));
  }

  async ngOnInit(): Promise<void> {
    this.loadTemplates();
    try {
      const [settings, status] = await Promise.all([
        firstValueFrom(this.service.get()),
        firstValueFrom(this.service.googleStatus()),
      ]);
      this.settings.set(settings);
      this.status.set(status);
      this.calendarId.setValue(settings.calendarId);
      this.spreadsheetId.setValue(settings.reportSpreadsheetId);
      this.worksheetTitle.setValue(settings.reportWorksheetTitle);
      this.perMonthSheet.setValue(!settings.reportWorksheetTitle);

      if (status.connected) {
        firstValueFrom(this.service.calendars())
          .then((c) => this.calendars.set(c))
          .catch(() => void 0);
        firstValueFrom(this.service.spreadsheets())
          .then((s) => this.spreadsheets.set(s))
          .catch(() => void 0);
        if (settings.reportSpreadsheetId) this.loadWorksheets(settings.reportSpreadsheetId);
      }
    } finally {
      this.loading.set(false);
    }
  }

  connect(): void {
    this.auth.loginWithGoogle();
  }

  async disconnect(): Promise<void> {
    await firstValueFrom(this.service.disconnectGoogle());
    this.status.set({ ...(this.status() as GoogleStatus), connected: false });
    this.calendars.set([]);
    this.spreadsheets.set([]);
    await this.auth.refresh();
    this.snack.open('Google disconnected', 'Dismiss', { duration: 4000 });
  }

  async saveCalendar(): Promise<void> {
    const chosen = this.calendars().find((c) => c.id === this.calendarId.value);
    this.settings.set(
      await firstValueFrom(
        this.service.update({
          ...(this.settings() as Settings),
          calendarId: this.calendarId.value,
          calendarSummary: chosen?.summary ?? null,
        }),
      ),
    );
    this.snack.open('Calendar saved', 'Dismiss', { duration: 3000 });
  }

  onSpreadsheetChange(): void {
    this.worksheetTitle.setValue(null);
    this.worksheets.set([]);
    if (this.spreadsheetId.value) this.loadWorksheets(this.spreadsheetId.value);
  }

  private loadWorksheets(id: string): void {
    firstValueFrom(this.service.spreadsheet(id))
      .then((d) => this.worksheets.set(d.worksheets))
      .catch(() => void 0);
  }

  async saveReportSettings(): Promise<void> {
    const chosen = this.spreadsheets().find((s) => s.id === this.spreadsheetId.value);
    this.settings.set(
      await firstValueFrom(
        this.service.update({
          ...(this.settings() as Settings),
          reportSpreadsheetId: this.spreadsheetId.value,
          reportSpreadsheetName: chosen?.name ?? null,
          reportWorksheetTitle: this.perMonthSheet.value ? null : this.worksheetTitle.value,
        }),
      ),
    );
    this.snack.open('Report settings saved', 'Dismiss', { duration: 3000 });
  }
}
