import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
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
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent {
  private readonly auth = inject(AuthService);
  private readonly breakpoints = inject(BreakpointObserver);

  /** Phones and small tablets get an overlay drawer instead of a permanent sidebar. */
  readonly handset = signal(false);

  readonly nav: NavItem[] = [
    { label: 'Dashboard', path: '/', icon: 'dashboard' },
    { label: 'Students', path: '/students', icon: 'group' },
    { label: 'Calendar', path: '/calendar', icon: 'calendar_month' },
    { label: 'Reports', path: '/reports', icon: 'receipt_long' },
    { label: 'Statistics', path: '/statistics', icon: 'insights' },
    { label: 'Settings', path: '/settings', icon: 'settings' },
  ];

  constructor() {
    this.breakpoints
      .observe([Breakpoints.Handset, Breakpoints.TabletPortrait])
      .pipe(takeUntilDestroyed())
      .subscribe((state) => this.handset.set(state.matches));
  }

  closeOnHandset(drawer: { close: () => void }): void {
    if (this.handset()) {
      drawer.close();
    }
  }

  displayName(): string {
    const u = this.auth.user();
    if (!u) return '';
    return u.firstName ? `${u.firstName} ${u.lastName ?? ''}`.trim() : u.email;
  }

  logout(): void {
    void this.auth.logout();
  }
}
