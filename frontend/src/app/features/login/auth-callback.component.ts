import { Component, inject, OnInit } from '@angular/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-auth-callback',
  imports: [MatProgressSpinnerModule],
  template: `
    <div class="wrap">
      <mat-spinner diameter="48" />
      <p class="muted">Finishing sign-in…</p>
    </div>
  `,
  styles: [
    `
      .wrap {
        min-height: 100vh;
        display: grid;
        place-items: center;
        gap: 16px;
      }
    `,
  ],
})
export class AuthCallbackComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  async ngOnInit(): Promise<void> {
    const user = await this.auth.refresh();
    await this.router.navigateByUrl(user ? '/' : '/login');
  }
}
