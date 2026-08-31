export interface CurrentUser {
  id: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
  googleConnected: boolean;
}
