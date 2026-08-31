import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  GoogleCalendarSummary,
  GoogleStatus,
  Settings,
  SpreadsheetDetail,
  SpreadsheetSummary,
} from './settings.model';

@Injectable({ providedIn: 'root' })
export class SettingsService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBaseUrl;

  get(): Observable<Settings> {
    return this.http.get<Settings>(`${this.api}/api/settings`);
  }

  update(settings: Settings): Observable<Settings> {
    return this.http.put<Settings>(`${this.api}/api/settings`, settings);
  }

  googleStatus(): Observable<GoogleStatus> {
    return this.http.get<GoogleStatus>(`${this.api}/api/integrations/google/status`);
  }

  calendars(): Observable<GoogleCalendarSummary[]> {
    return this.http.get<GoogleCalendarSummary[]>(`${this.api}/api/integrations/google/calendars`);
  }

  spreadsheets(): Observable<SpreadsheetSummary[]> {
    return this.http.get<SpreadsheetSummary[]>(`${this.api}/api/integrations/google/spreadsheets`);
  }

  spreadsheet(id: string): Observable<SpreadsheetDetail> {
    return this.http.get<SpreadsheetDetail>(
      `${this.api}/api/integrations/google/spreadsheets/${encodeURIComponent(id)}`,
    );
  }

  disconnectGoogle(): Observable<void> {
    return this.http.post<void>(`${this.api}/api/integrations/google/disconnect`, {});
  }
}
