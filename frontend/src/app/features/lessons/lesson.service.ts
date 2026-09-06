import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CalendarSyncSummary,
  GoogleImportPreview,
  GoogleImportRequest,
  GoogleImportResult,
  Lesson,
  LessonRequest,
  LessonStatus,
  LessonStudentOverview,
  RepeatLessonRequest,
} from './lesson.model';

@Injectable({ providedIn: 'root' })
export class LessonService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/lessons`;

  list(
    opts: {
      studentId?: string;
      status?: LessonStatus;
      from?: string;
      to?: string;
    } = {},
  ): Observable<Lesson[]> {
    let params = new HttpParams();
    for (const [k, v] of Object.entries(opts)) {
      if (v) params = params.set(k, v);
    }
    return this.http.get<Lesson[]>(this.base, { params });
  }

  get(id: string): Observable<Lesson> {
    return this.http.get<Lesson>(`${this.base}/${id}`);
  }

  studentOverview(studentId: string): Observable<LessonStudentOverview> {
    return this.http.get<LessonStudentOverview>(`${this.base}/student/${studentId}/overview`);
  }

  create(request: LessonRequest): Observable<Lesson> {
    return this.http.post<Lesson>(this.base, request);
  }

  update(id: string, request: LessonRequest): Observable<Lesson> {
    return this.http.put<Lesson>(`${this.base}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  complete(id: string): Observable<Lesson> {
    return this.http.post<Lesson>(`${this.base}/${id}/complete`, {});
  }

  cancel(id: string): Observable<Lesson> {
    return this.http.post<Lesson>(`${this.base}/${id}/cancel`, {});
  }

  noShow(id: string): Observable<Lesson> {
    return this.http.post<Lesson>(`${this.base}/${id}/no-show`, {});
  }

  /** Back to PLANNED — undoes a completion / cancellation and restores the calendar event. */
  replan(id: string): Observable<Lesson> {
    return this.http.post<Lesson>(`${this.base}/${id}/replan`, {});
  }

  repeat(id: string, request: RepeatLessonRequest): Observable<Lesson[]> {
    return this.http.post<Lesson[]>(`${this.base}/${id}/repeat`, request);
  }

  syncCalendar(id: string): Observable<Lesson> {
    return this.http.post<Lesson>(`${this.base}/${id}/sync-calendar`, {});
  }

  /** Re-push every upcoming (or previously failed) lesson of one student. */
  syncStudentCalendar(studentId: string): Observable<CalendarSyncSummary> {
    return this.http.post<CalendarSyncSummary>(
      `${this.base}/student/${studentId}/sync-calendar`,
      {},
    );
  }

  // --- Google Calendar -> app import ---

  googleImportPreview(from: string, to: string): Observable<GoogleImportPreview> {
    return this.http.get<GoogleImportPreview>(`${this.base}/google/preview`, {
      params: new HttpParams().set('from', from).set('to', to),
    });
  }

  googleImport(request: GoogleImportRequest): Observable<GoogleImportResult> {
    return this.http.post<GoogleImportResult>(`${this.base}/google/import`, request);
  }
}
