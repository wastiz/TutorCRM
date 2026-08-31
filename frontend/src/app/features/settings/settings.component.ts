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
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
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
  ],
  template: `
    <div class="page">
      <div class="page-header"><h1>Settings</h1></div>

      <mat-card>
        <mat-card-header><mat-card-title>Google integration</mat-card-title></mat-card-header>
        <mat-card-content>
          @if (loading()) {
            <mat-progress-bar mode="indeterminate" />
          } @else {
            <div class="row">
              <mat-icon [class.ok]="status()?.connected">
                {{ status()?.connected ? 'check_circle' : 'cancel' }}
              </mat-icon>
              <div>
                <div><strong>Google account:</strong> {{ status()?.connected ? 'Connected' : 'Not connected' }}</div>
                @if (status()?.connected) {
                  <div class="muted small">{{ scopeList() }}</div>
                }
              </div>
              <span class="spacer"></span>
              <button mat-stroked-button (click)="connect()">
                {{ status()?.connected ? 'Reconnect Google' : 'Connect Google' }}
              </button>
              @if (status()?.connected) {
                <button mat-stroked-button color="warn" (click)="disconnect()">Disconnect</button>
              }
            </div>

            @if (status()?.connected) {
              <mat-form-field appearance="outline" class="wide">
                <mat-label>Calendar for lessons</mat-label>
                <mat-select [formControl]="calendarId" (selectionChange)="saveCalendar()">
                  <mat-option [value]="null">— none (don't sync) —</mat-option>
                  @for (c of calendars(); track c.id) {
                    <mat-option [value]="c.id" [disabled]="!c.writable">
                      {{ c.summary }}@if (!c.writable) { (read-only) }
                    </mat-option>
                  }
                </mat-select>
                <mat-hint>Lessons you create are mirrored here. The calendar may belong to another account you can edit.</mat-hint>
              </mat-form-field>
            }
          }
        </mat-card-content>
      </mat-card>

      <mat-card>
        <mat-card-header><mat-card-title>Monthly report spreadsheet</mat-card-title></mat-card-header>
        <mat-card-content>
          <p class="muted">
            The existing Google Sheet the <a routerLink="/reports">monthly report</a> is exported to.
          </p>

          @if (!status()?.connected) {
            <p class="muted">Connect Google above to pick a spreadsheet.</p>
          } @else {
            <mat-form-field appearance="outline" class="wide">
              <mat-label>Spreadsheet</mat-label>
              <mat-select [formControl]="spreadsheetId" (selectionChange)="onSpreadsheetChange()">
                <mat-option [value]="null">— none —</mat-option>
                @for (s of spreadsheets(); track s.id) {
                  <mat-option [value]="s.id">{{ s.name }}</mat-option>
                }
              </mat-select>
            </mat-form-field>

            <mat-checkbox [formControl]="perMonthSheet">Write each month to its own worksheet (e.g. «август 26»)</mat-checkbox>

            @if (!perMonthSheet.value && spreadsheetId.value) {
              <mat-form-field appearance="outline" class="wide">
                <mat-label>Worksheet</mat-label>
                <mat-select [formControl]="worksheetTitle">
                  @for (w of worksheets(); track w.title) {
                    <mat-option [value]="w.title">{{ w.title }}</mat-option>
                  }
                </mat-select>
              </mat-form-field>
            }

            <button mat-flat-button color="primary" (click)="saveReportSettings()">Save</button>
          }
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [
    `
      mat-card { margin-bottom: 16px; }
      .row { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
      .row mat-icon.ok { color: #2e7d32; }
      .spacer { flex: 1 1 auto; }
      .wide { width: 100%; display: block; }
      .small { font-size: 12px; }
      mat-checkbox { display: block; margin: 8px 0 12px; }
    `,
  ],
})
export class SettingsComponent implements OnInit {
  private readonly service = inject(SettingsService);
  private readonly auth = inject(AuthService);
  private readonly snack = inject(MatSnackBar);

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

  async ngOnInit(): Promise<void> {
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
        firstValueFrom(this.service.calendars()).then((c) => this.calendars.set(c)).catch(() => void 0);
        firstValueFrom(this.service.spreadsheets()).then((s) => this.spreadsheets.set(s)).catch(() => void 0);
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
