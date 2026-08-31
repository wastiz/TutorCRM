import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'auth/callback',
    loadComponent: () =>
      import('./features/login/auth-callback.component').then((m) => m.AuthCallbackComponent),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./core/layout/shell.component').then((m) => m.ShellComponent),
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      {
        path: 'students',
        loadComponent: () =>
          import('./features/students/students-list.component').then((m) => m.StudentsListComponent),
      },
      {
        path: 'students/new',
        loadComponent: () =>
          import('./features/students/student-create.component').then((m) => m.StudentCreateComponent),
      },
      {
        path: 'students/:id',
        loadComponent: () =>
          import('./features/students/student-detail.component').then((m) => m.StudentDetailComponent),
      },
      {
        path: 'students/:id/edit',
        loadComponent: () =>
          import('./features/students/student-edit.component').then((m) => m.StudentEditComponent),
      },
      {
        path: 'calendar',
        loadComponent: () =>
          import('./features/calendar/calendar.component').then((m) => m.CalendarComponent),
      },
      {
        path: 'reports',
        loadComponent: () =>
          import('./features/reports/reports.component').then((m) => m.ReportsComponent),
      },
      {
        path: 'settings',
        loadComponent: () =>
          import('./features/settings/settings.component').then((m) => m.SettingsComponent),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
