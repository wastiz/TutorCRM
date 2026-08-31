import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-login',
  imports: [MatCardModule, MatButtonModule, MatIconModule],
  template: `
    <div class="login-wrap">
      <mat-card class="login-card">
        <mat-card-header>
          <mat-card-title>Tutor Management System</mat-card-title>
          <mat-card-subtitle>Sign in to continue</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <p class="muted">
            Sign in with the Google account that has access to your calendar and reporting spreadsheet.
          </p>
        </mat-card-content>
        <mat-card-actions>
          <button mat-flat-button color="primary" (click)="login()">
            <mat-icon>login</mat-icon>
            Sign in with Google
          </button>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: [
    `
      .login-wrap {
        min-height: 100vh;
        display: grid;
        place-items: center;
        background: #f0f2f5;
      }
      .login-card {
        width: 380px;
        padding: 12px;
      }
      mat-card-content {
        margin: 12px 0;
      }
    `,
  ],
})
export class LoginComponent {
  private readonly auth = inject(AuthService);

  login(): void {
    this.auth.loginWithGoogle();
  }
}
