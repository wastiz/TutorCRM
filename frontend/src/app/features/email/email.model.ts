export type EmailTemplateKind = 'GENERAL' | 'WELCOME' | 'LESSON' | 'MONTHLY_SUMMARY';

export const EMAIL_TEMPLATE_KINDS: EmailTemplateKind[] = [
  'GENERAL',
  'WELCOME',
  'LESSON',
  'MONTHLY_SUMMARY',
];

export const EMAIL_TEMPLATE_KIND_LABEL: Record<EmailTemplateKind, string> = {
  GENERAL: 'General',
  WELCOME: 'Welcome',
  LESSON: 'About a lesson',
  MONTHLY_SUMMARY: 'Monthly summary',
};

export interface EmailTemplate {
  id: string;
  name: string;
  kind: EmailTemplateKind;
  subject: string;
  body: string;
  createdAt: string;
  updatedAt: string;
}

export interface EmailTemplateRequest {
  name: string;
  kind: EmailTemplateKind;
  subject: string;
  body: string;
}

export interface EmailPlaceholder {
  name: string;
  description: string;
}

export interface SendEmailRequest {
  templateId?: string | null;
  studentId: string;
  lessonId?: string | null;
  to?: string | null;
  subject?: string | null;
  body?: string | null;
}

export interface EmailPreview {
  to: string | null;
  subject: string;
  body: string;
  unresolvedPlaceholders: string[];
  warnings: string[];
}

export interface EmailSendResult {
  to: string;
  subject: string;
  providerMessageId: string;
  sentAt: string;
}
