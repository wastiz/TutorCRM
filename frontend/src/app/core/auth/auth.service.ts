import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CurrentUser } from './current-user.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly _user = signal<CurrentUser | null>(null);
  private readonly _loaded = signal(false);

  readonly user = this._user.asReadonly();
  readonly loaded = this._loaded.asReadonly();
  readonly isAuthenticated = computed(() => this._user() !== null);

  /** Called once at app start (provideAppInitializer). Never rejects. */
  async init(): Promise<void> {
    await this.refresh();
  }

  async refresh(): Promise<CurrentUser | null> {
    try {
      const user = await firstValueFrom(
        this.http.get<CurrentUser>(`${environment.apiBaseUrl}/api/auth/me`),
      );
      this._user.set(user);
      return user;
    } catch {
      this._user.set(null);
      return null;
    } finally {
      this._loaded.set(true);
    }
  }

  loginWithGoogle(): void {
    window.location.href = `${environment.apiBaseUrl}/oauth2/authorization/google`;
  }

  /**
   * Signs in as the seeded demo tutor. The endpoint only exists when the backend runs with the
   * `local` profile; the button that calls this is hidden in production builds.
   */
  loginAsDevTutor(): void {
    window.location.href = `${environment.apiBaseUrl}/dev/login`;
  }

  async logout(): Promise<void> {
    try {
      await firstValueFrom(this.http.post(`${environment.apiBaseUrl}/api/auth/logout`, {}));
    } finally {
      this._user.set(null);
      window.location.href = '/login';
    }
  }
}
