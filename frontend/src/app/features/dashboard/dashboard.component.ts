import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';
import { Dashboard } from './dashboard.model';
import { DashboardService } from './dashboard.service';

@Component({
  selector: 'app-dashboard',
  imports: [
    RouterLink,
    DatePipe,
    DecimalPipe,
    MatCardModule,
    MatIconModule,
    MatListModule,
    MatProgressBarModule,
  ],
  template: `
    <div class="page">
      <div class="page-header"><h1>Dashboard</h1></div>

      @if (loading()) {
        <mat-progress-bar mode="indeterminate" />
      } @else if (data(); as d) {
        <div class="tiles">
          <div class="tile">
            <mat-icon>group</mat-icon>
            <span class="value">{{ d.activeStudents }}</span>
            <span class="label">Active students</span>
          </div>
          <div class="tile">
            <mat-icon>today</mat-icon>
            <span class="value">{{ d.todaysLessons }}</span>
            <span class="label">Lessons today</span>
          </div>
          <div class="tile">
            <mat-icon>date_range</mat-icon>
            <span class="value">{{ d.thisWeekLessons }}</span>
            <span class="label">Lessons this week</span>
          </div>
          <div class="tile">
            <mat-icon>check_circle</mat-icon>
            <span class="value">{{ d.thisMonthCompletedLessons }}</span>
            <span class="label">Completed this month</span>
          </div>
          <div class="tile accent">
            <mat-icon>payments</mat-icon>
            <span class="value">€{{ d.thisMonthExpectedEarnings | number: '1.0-2' }}</span>
            <span class="label">Expected earnings (this month)</span>
          </div>
          <div class="tile">
            <mat-icon>savings</mat-icon>
            <span class="value">€{{ d.thisMonthEarnings | number: '1.0-2' }}</span>
            <span class="label">Earned so far this month</span>
          </div>
        </div>

        <mat-card class="upcoming">
          <mat-card-header><mat-card-title>Upcoming lessons</mat-card-title></mat-card-header>
          <mat-card-content>
            @if (d.upcomingLessons.length) {
              <mat-list>
                @for (l of d.upcomingLessons; track l.id) {
                  <mat-list-item>
                    <span matListItemTitle>
                      <a [routerLink]="['/students', l.studentId]">{{ l.studentName }}</a>
                    </span>
                    <span matListItemLine>
                      {{ l.startTime | date: 'EEE d MMM, HH:mm' }} – {{ l.endTime | date: 'HH:mm' }} · €{{ l.price }}
                    </span>
                  </mat-list-item>
                }
              </mat-list>
            } @else {
              <p class="muted">Nothing scheduled. Add lessons from a student or the calendar.</p>
            }
          </mat-card-content>
        </mat-card>
      }
    </div>
  `,
  styles: [
    `
      .tiles {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
        gap: 16px;
        margin-bottom: 24px;
      }
      .tile {
        background: #fff;
        border-radius: 10px;
        padding: 18px 20px;
        display: flex;
        flex-direction: column;
        gap: 2px;
      }
      .tile mat-icon {
        color: rgba(0, 0, 0, 0.4);
      }
      .tile .value {
        font-size: 26px;
        font-weight: 600;
      }
      .tile .label {
        font-size: 12px;
        color: rgba(0, 0, 0, 0.55);
      }
      .tile.accent {
        background: #e8eaf6;
      }
      .upcoming a {
        text-decoration: none;
      }
    `,
  ],
})
export class DashboardComponent implements OnInit {
  private readonly service = inject(DashboardService);

  readonly data = signal<Dashboard | null>(null);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.service.summary().subscribe({
      next: (d) => {
        this.data.set(d);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
