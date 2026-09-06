import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  EmailPlaceholder,
  EmailPreview,
  EmailSendResult,
  EmailTemplate,
  EmailTemplateRequest,
  SendEmailRequest,
} from './email.model';

@Injectable({ providedIn: 'root' })
export class EmailService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/email`;

  templates(): Observable<EmailTemplate[]> {
    return this.http.get<EmailTemplate[]>(`${this.base}/templates`);
  }

  createTemplate(request: EmailTemplateRequest): Observable<EmailTemplate> {
    return this.http.post<EmailTemplate>(`${this.base}/templates`, request);
  }

  updateTemplate(id: string, request: EmailTemplateRequest): Observable<EmailTemplate> {
    return this.http.put<EmailTemplate>(`${this.base}/templates/${id}`, request);
  }

  deleteTemplate(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/templates/${id}`);
  }

  placeholders(): Observable<EmailPlaceholder[]> {
    return this.http.get<EmailPlaceholder[]>(`${this.base}/placeholders`);
  }

  preview(request: SendEmailRequest): Observable<EmailPreview> {
    return this.http.post<EmailPreview>(`${this.base}/preview`, request);
  }

  send(request: SendEmailRequest): Observable<EmailSendResult> {
    return this.http.post<EmailSendResult>(`${this.base}/send`, request);
  }
}
