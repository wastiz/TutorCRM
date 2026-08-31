export interface Settings {
  calendarId: string | null;
  calendarSummary: string | null;
  reportSpreadsheetId: string | null;
  reportSpreadsheetName: string | null;
  reportWorksheetTitle: string | null;
}

export interface GoogleStatus {
  connected: boolean;
  provider: string | null;
  accessTokenExpiresAt: string | null;
  grantedScopes: string[];
  connectedAt: string | null;
  updatedAt: string | null;
}

export interface GoogleCalendarSummary {
  id: string;
  summary: string;
  description: string | null;
  accessRole: string;
  primary: boolean;
  writable: boolean;
}

export interface SpreadsheetSummary {
  id: string;
  name: string;
  url: string | null;
}

export interface SpreadsheetWorksheet {
  title: string;
  sheetId: number | null;
}

export interface SpreadsheetDetail {
  id: string;
  name: string;
  url: string | null;
  worksheets: SpreadsheetWorksheet[];
}
