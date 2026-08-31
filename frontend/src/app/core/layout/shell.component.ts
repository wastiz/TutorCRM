import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../auth/auth.service';

interface NavItem {
  label: string;
  path: string;
  icon: string;
}

@Component({
  selector: 'app-shell',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatSidenavModule,
    MatToolbarModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
  ],
  template: `
    <mat-sidenav-container class="shell">
      <mat-sidenav mode="side" opened class="shell-nav">
        <div class="brand">
          <mat-icon>school</mat-icon>
          <span>Tutor MS</span>
        </div>
        <mat-nav-list>
          @for (item of nav; track item.path) {
            <a
              mat-list-item
              [routerLink]="item.path"
              routerLinkActive="active-link"
              [routerLinkActiveOptions]="{ exact: item.path === '/' }"
            >
              <mat-icon matListItemIcon>{{ item.icon }}</mat-icon>
              <span matListItemTitle>{{ item.label }}</span>
            </a>
          }
        </mat-nav-list>
      </mat-sidenav>

      <mat-sidenav-content>
        <mat-toolbar color="primary" class="shell-toolbar">
          <span class="spacer"></span>
          <button mat-button [matMenuTriggerFor]="menu">
            <mat-icon>account_circle</mat-icon>
            {{ displayName() }}
          </button>
          <mat-menu #menu="matMenu">
            <button mat-menu-item routerLink="/settings">
              <mat-icon>settings</mat-icon>
              <span>Settings</span>
            </button>
            <button mat-menu-item (click)="logout()">
              <mat-icon>logout</mat-icon>
              <span>Sign out</span>
            </button>
          </mat-menu>
        </mat-toolbar>

        <router-outlet />
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styles: [
    `
      .shell {
        height: 100vh;
      }
      .shell-nav {
        width: 220px;
        border-right: 1px solid rgba(0, 0, 0, 0.08);
      }
      .brand {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 18px;
        font-weight: 600;
        padding: 16px;
      }
      .shell-toolbar {
        position: sticky;
        top: 0;
        z-index: 2;
      }
      .spacer {
        flex: 1 1 auto;
      }
      .active-link {
        background: rgba(63, 81, 181, 0.12);
      }
    `,
  ],
})
export class ShellComponent {
  private readonly auth = inject(AuthService);

  readonly nav: NavItem[] = [
    { label: 'Dashboard', path: '/', icon: 'dashboard' },
    { label: 'Students', path: '/students', icon: 'group' },
    { label: 'Calendar', path: '/calendar', icon: 'calendar_month' },
    { label: 'Reports', path: '/reports', icon: 'receipt_long' },
    { label: 'Settings', path: '/settings', icon: 'settings' },
  ];

  displayName(): string {
    const u = this.auth.user();
    if (!u) return '';
    return u.firstName ? `${u.firstName} ${u.lastName ?? ''}`.trim() : u.email;
  }

  logout(): void {
    void this.auth.logout();
  }
}
