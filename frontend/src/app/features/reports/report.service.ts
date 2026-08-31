import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MonthlyReport } from './report.model';

export interface ExportResult {
  spreadsheetId: string;
  spreadsheetUrl: string;
  worksheetTitle: string;
  updated: boolean;
}

@Injectable({ providedIn: 'root' })
export class ReportService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/reports`;

  monthly(month: string): Observable<MonthlyReport> {
    return this.http.get<MonthlyReport>(`${this.base}/monthly`, {
      params: new HttpParams().set('month', month),
    });
  }

  export(month: string, overwrite = false): Observable<ExportResult> {
    return this.http.post<ExportResult>(`${this.base}/monthly/export`, { month, overwrite });
  }
}
