import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { GoogleCalendarSummary, GoogleStatus, Settings } from './settings.model';
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
            Choose the existing Google Sheet used for monthly payout reports on the
            <a routerLink="/reports">Reports</a> page. Configured during export (Phase 7).
          </p>
          <div class="grid">
            <mat-form-field appearance="outline">
              <mat-label>Spreadsheet ID</mat-label>
              <input matInput [formControl]="spreadsheetId" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Spreadsheet name</mat-label>
              <input matInput [formControl]="spreadsheetName" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Fixed worksheet title (optional)</mat-label>
              <input matInput [formControl]="worksheetTitle" placeholder="leave blank for per-month sheets" />
            </mat-form-field>
          </div>
          <button mat-flat-button color="primary" (click)="saveReportSettings()">Save</button>
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
      .wide { width: 100%; }
      .small { font-size: 12px; }
      .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 12px; margin: 12px 0; }
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
  private readonly settings = signal<Settings | null>(null);

  readonly calendarId = new FormControl<string | null>(null);
  readonly spreadsheetId = new FormControl<string>('', { nonNullable: true });
  readonly spreadsheetName = new FormControl<string>('', { nonNullable: true });
  readonly worksheetTitle = new FormControl<string>('', { nonNullable: true });

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
      this.spreadsheetId.setValue(settings.reportSpreadsheetId ?? '');
      this.spreadsheetName.setValue(settings.reportSpreadsheetName ?? '');
      this.worksheetTitle.setValue(settings.reportWorksheetTitle ?? '');
      if (status.connected) {
        firstValueFrom(this.service.calendars())
          .then((c) => this.calendars.set(c))
          .catch(() => void 0);
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
    await this.auth.refresh();
    this.snack.open('Google disconnected', 'Dismiss', { duration: 4000 });
  }

  async saveCalendar(): Promise<void> {
    const chosen = this.calendars().find((c) => c.id === this.calendarId.value);
    const next: Settings = {
      ...(this.settings() as Settings),
      calendarId: this.calendarId.value,
      calendarSummary: chosen?.summary ?? null,
    };
    this.settings.set(await firstValueFrom(this.service.update(next)));
    this.snack.open('Calendar saved', 'Dismiss', { duration: 3000 });
  }

  async saveReportSettings(): Promise<void> {
    const next: Settings = {
      ...(this.settings() as Settings),
      reportSpreadsheetId: this.spreadsheetId.value || null,
      reportSpreadsheetName: this.spreadsheetName.value || null,
      reportWorksheetTitle: this.worksheetTitle.value || null,
    };
    this.settings.set(await firstValueFrom(this.service.update(next)));
    this.snack.open('Report settings saved', 'Dismiss', { duration: 3000 });
  }
}
