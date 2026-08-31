import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  DuplicateCheckRequest,
  Student,
  StudentImportPreview,
  StudentRequest,
  StudentStatus,
  StudentSummary,
} from './student.model';

@Injectable({ providedIn: 'root' })
export class StudentService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/students`;

  list(search?: string, status?: StudentStatus | null): Observable<StudentSummary[]> {
    let params = new HttpParams();
    if (search) params = params.set('search', search);
    if (status) params = params.set('status', status);
    return this.http.get<StudentSummary[]>(this.base, { params });
  }

  get(id: string): Observable<Student> {
    return this.http.get<Student>(`${this.base}/${id}`);
  }

  create(request: StudentRequest): Observable<Student> {
    return this.http.post<Student>(this.base, request);
  }

  update(id: string, request: StudentRequest): Observable<Student> {
    return this.http.put<Student>(`${this.base}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  archive(id: string): Observable<Student> {
    return this.http.post<Student>(`${this.base}/${id}/archive`, {});
  }

  checkDuplicates(request: DuplicateCheckRequest): Observable<{ duplicates: StudentSummary[] }> {
    return this.http.post<{ duplicates: StudentSummary[] }>(`${this.base}/duplicates`, request);
  }

  parseImport(rawText: string): Observable<StudentImportPreview> {
    return this.http.post<StudentImportPreview>(`${this.base}/import/parse`, { rawText });
  }
}
