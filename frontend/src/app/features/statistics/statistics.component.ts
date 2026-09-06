import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';
import { Statistics } from './statistics.model';
import { StatisticsService } from './statistics.service';

@Component({
  selector: 'app-statistics',
  imports: [RouterLink, DatePipe, DecimalPipe, MatCardModule, MatIconModule, MatProgressBarModule],
  templateUrl: './statistics.component.html',
  styleUrl: './statistics.component.scss',
})
export class StatisticsComponent implements OnInit {
  private readonly service = inject(StatisticsService);

  readonly data = signal<Statistics | null>(null);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.service.overview().subscribe({
      next: (d) => {
        this.data.set(d);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  /** "2026-09" → "Sep 2026". */
  monthLabel(month: string): string {
    const [year, m] = month.split('-').map(Number);
    return new Date(year, m - 1, 1).toLocaleDateString(undefined, {
      month: 'short',
      year: 'numeric',
    });
  }

  /** Bar width relative to the busiest month, so the shape is readable. */
  share(count: number): number {
    const peak = Math.max(
      1,
      ...(this.data()?.byMonth ?? []).map((m) => m.completed + m.cancelled + m.noShow),
    );
    return (count / peak) * 100;
  }
}
