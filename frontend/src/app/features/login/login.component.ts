import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { environment } from '../../../environments/environment';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-login',
  imports: [MatCardModule, MatButtonModule, MatIconModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  private readonly auth = inject(AuthService);

  /** Hides the dev shortcut in a production bundle. */
  readonly production = environment.production;

  login(): void {
    this.auth.loginWithGoogle();
  }

  devLogin(): void {
    this.auth.loginAsDevTutor();
  }
}
