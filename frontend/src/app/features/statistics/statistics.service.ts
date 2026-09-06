import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Statistics } from './statistics.model';

@Injectable({ providedIn: 'root' })
export class StatisticsService {
  private readonly http = inject(HttpClient);

  overview(): Observable<Statistics> {
    return this.http.get<Statistics>(`${environment.apiBaseUrl}/api/statistics`);
  }
}
