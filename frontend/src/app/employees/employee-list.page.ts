import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, ParamMap, Params, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { catchError, debounceTime, distinctUntilChanged, map, of, startWith, Subject, switchMap, combineLatest, tap } from 'rxjs';
import { EmployeeApi } from './employee-api';
import { Employee, EmployeeQuery, EmployeeStatus, Page } from './employee.model';
import { errorMessage } from '../core/api-error';
import { countryName, formatMoney } from '../core/format';

const DEFAULT_SIZE = 20;
const DEFAULT_SORT = 'fullName,asc';

function toQuery(params: ParamMap): EmployeeQuery {
  return {
    q: params.get('q') ?? '',
    country: params.get('country') ?? '',
    department: params.get('department') ?? '',
    jobTitle: params.get('jobTitle') ?? '',
    status: (params.get('status') ?? '') as EmployeeStatus | '',
    page: Number(params.get('page') ?? 0),
    size: Number(params.get('size') ?? DEFAULT_SIZE),
    sort: params.get('sort') ?? DEFAULT_SORT,
  };
}

/**
 * Searchable, filterable, paged directory. All criteria live in the URL, so a filtered view can
 * be bookmarked or shared and the back button undoes a filter change.
 */
@Component({
  selector: 'app-employee-list',
  imports: [
    ReactiveFormsModule, RouterLink, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule,
    MatPaginatorModule, MatProgressBarModule, MatSelectModule, MatSortModule, MatTableModule,
  ],
  template: `
    <header class="page-header">
      <h1>Employees</h1>
      @if (page(); as p) {
        <span class="count">{{ p.page.totalElements.toLocaleString() }} found</span>
      }
    </header>

    <section class="filters" aria-label="Search and filters">
      <mat-form-field class="search">
        <mat-label>Search</mat-label>
        <mat-icon matPrefix>search</mat-icon>
        <input matInput type="search" [formControl]="search" placeholder="Name, employee code or email" />
      </mat-form-field>
      <mat-form-field>
        <mat-label>Country</mat-label>
        <mat-select aria-label="Country" [value]="query().country" (selectionChange)="filter('country', $event.value)">
          <mat-option value="">All countries</mat-option>
          @for (code of filters()?.countries; track code) {
            <mat-option [value]="code">{{ countryName(code) }}</mat-option>
          }
        </mat-select>
      </mat-form-field>
      <mat-form-field>
        <mat-label>Department</mat-label>
        <mat-select aria-label="Department" [value]="query().department" (selectionChange)="filter('department', $event.value)">
          <mat-option value="">All departments</mat-option>
          @for (department of filters()?.departments; track department) {
            <mat-option [value]="department">{{ department }}</mat-option>
          }
        </mat-select>
      </mat-form-field>
      <mat-form-field>
        <mat-label>Job title</mat-label>
        <mat-select aria-label="Job title" [value]="query().jobTitle" (selectionChange)="filter('jobTitle', $event.value)">
          <mat-option value="">All job titles</mat-option>
          @for (title of filters()?.jobTitles; track title) {
            <mat-option [value]="title">{{ title }}</mat-option>
          }
        </mat-select>
      </mat-form-field>
      <mat-form-field>
        <mat-label>Status</mat-label>
        <mat-select aria-label="Status" [value]="query().status" (selectionChange)="filter('status', $event.value)">
          <mat-option value="">Any status</mat-option>
          <mat-option value="ACTIVE">Active</mat-option>
          <mat-option value="TERMINATED">Terminated</mat-option>
        </mat-select>
      </mat-form-field>
    </section>

    <div class="table-card">
      @if (loading()) {
        <mat-progress-bar mode="indeterminate" class="loading" />
      }
      @if (error()) {
        <div class="message" role="alert">
          <p>{{ error() }}</p>
          <button mat-stroked-button (click)="reload.next()">Try again</button>
        </div>
      } @else if (page()?.content?.length === 0) {
        <p class="message">No employees match these filters.</p>
      } @else if (page(); as p) {
        <div class="table-scroll">
          <table mat-table [dataSource]="p.content" matSort [matSortActive]="sortActive()" [matSortDirection]="sortDirection()"
                 (matSortChange)="sortBy($event)" matSortDisableClear>
            <ng-container matColumnDef="fullName">
              <th mat-header-cell *matHeaderCellDef mat-sort-header>Employee</th>
              <td mat-cell *matCellDef="let e">
                <a class="name" [routerLink]="['/employees', e.id]">{{ e.fullName }}</a>
                <div class="secondary">{{ e.employeeCode }}</div>
              </td>
            </ng-container>
            <ng-container matColumnDef="jobTitle">
              <th mat-header-cell *matHeaderCellDef mat-sort-header>Job title</th>
              <td mat-cell *matCellDef="let e">{{ e.jobTitle }}</td>
            </ng-container>
            <ng-container matColumnDef="department">
              <th mat-header-cell *matHeaderCellDef mat-sort-header>Department</th>
              <td mat-cell *matCellDef="let e">{{ e.department }}</td>
            </ng-container>
            <ng-container matColumnDef="country">
              <th mat-header-cell *matHeaderCellDef mat-sort-header>Country</th>
              <td mat-cell *matCellDef="let e">{{ countryName(e.country) }}</td>
            </ng-container>
            <ng-container matColumnDef="salary">
              <th mat-header-cell *matHeaderCellDef class="numeric">Annual salary</th>
              <td mat-cell *matCellDef="let e" class="numeric">
                {{ e.currentSalary ? formatMoney(e.currentSalary.amount, e.currentSalary.currency) : '—' }}
              </td>
            </ng-container>
            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef mat-sort-header>Status</th>
              <td mat-cell *matCellDef="let e">
                <span class="status" [class.terminated]="e.status === 'TERMINATED'">
                  {{ e.status === 'TERMINATED' ? 'Terminated' : 'Active' }}
                </span>
              </td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="columns"></tr>
            <tr mat-row *matRowDef="let row; columns: columns" class="row" (click)="open(row)"></tr>
          </table>
        </div>
        <mat-paginator [length]="p.page.totalElements" [pageIndex]="p.page.number" [pageSize]="p.page.size"
                       [pageSizeOptions]="[20, 50, 100]" (page)="paginate($event)" aria-label="Pages" />
      }
    </div>
  `,
  styles: `
    .page-header { display: flex; align-items: baseline; gap: 16px; margin-bottom: 16px; }
    h1 { font: var(--mat-sys-headline-small); margin: 0; }
    .count { color: var(--mat-sys-on-surface-variant); }
    .filters { display: flex; flex-wrap: wrap; gap: 0 12px; }
    .filters mat-form-field { flex: 1 1 160px; }
    .filters .search { flex: 2 1 280px; }
    .table-card {
      position: relative;
      border: 1px solid var(--mat-sys-outline-variant);
      border-radius: var(--mat-sys-corner-medium);
      overflow: hidden;
      min-height: 120px;
    }
    .loading { position: absolute; top: 0; left: 0; right: 0; z-index: 1; }
    .table-scroll { overflow-x: auto; }
    table { width: 100%; }
    .row { cursor: pointer; }
    .row:hover { background: var(--mat-sys-surface-container-high); }
    .name { color: var(--mat-sys-primary); font-weight: 500; text-decoration: none; }
    .name:hover { text-decoration: underline; }
    .secondary { color: var(--mat-sys-on-surface-variant); font: var(--mat-sys-body-small); }
    .numeric { text-align: right; font-variant-numeric: tabular-nums; white-space: nowrap; }
    .status {
      display: inline-block;
      padding: 2px 10px;
      border-radius: 999px;
      font: var(--mat-sys-label-medium);
      background: var(--mat-sys-secondary-container);
      color: var(--mat-sys-on-secondary-container);
    }
    .status.terminated { background: var(--mat-sys-error-container); color: var(--mat-sys-on-error-container); }
    .message { padding: 32px 16px; text-align: center; color: var(--mat-sys-on-surface-variant); }
  `,
})
export class EmployeeListPage {
  private readonly api = inject(EmployeeApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly columns = ['fullName', 'jobTitle', 'department', 'country', 'salary', 'status'];
  protected readonly countryName = countryName;
  protected readonly formatMoney = formatMoney;

  private readonly query$ = this.route.queryParamMap.pipe(map(toQuery));
  protected readonly query = toSignal(this.query$, { initialValue: toQuery(this.route.snapshot.queryParamMap) });
  protected readonly filters = toSignal(this.api.filters().pipe(catchError(() => of(null))), { initialValue: null });

  protected readonly page = signal<Page<Employee> | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal('');
  protected readonly reload = new Subject<void>();

  protected readonly search = new FormControl(this.query().q ?? '', { nonNullable: true });
  protected readonly sortActive = () => this.query().sort!.split(',')[0];
  protected readonly sortDirection = () => this.query().sort!.split(',')[1] as 'asc' | 'desc';

  constructor() {
    // switchMap drops a slow earlier answer if the criteria changed meanwhile.
    combineLatest([this.query$, this.reload.pipe(startWith(undefined))])
      .pipe(
        tap(() => this.loading.set(true)),
        switchMap(([query]) =>
          this.api.list(query).pipe(
            catchError(e => {
              this.error.set(errorMessage(e, 'Could not load employees.'));
              return of(null);
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe(result => {
        this.loading.set(false);
        if (result) {
          this.page.set(result);
          this.error.set('');
        }
      });

    this.search.valueChanges
      .pipe(debounceTime(300), map(v => v.trim()), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe(q => this.navigate({ q: q || null, page: null }, true));

    // Back/forward can change the URL under us; keep the box in step without re-triggering a search.
    this.query$.pipe(takeUntilDestroyed()).subscribe(q => {
      if (q.q !== this.search.value.trim()) this.search.setValue(q.q ?? '', { emitEvent: false });
    });
  }

  protected filter(key: 'country' | 'department' | 'jobTitle' | 'status', value: string): void {
    this.navigate({ [key]: value || null, page: null });
  }

  protected sortBy(sort: Sort): void {
    const value = `${sort.active},${sort.direction || 'asc'}`;
    this.navigate({ sort: value === DEFAULT_SORT ? null : value, page: null });
  }

  protected paginate(event: PageEvent): void {
    this.navigate({ page: event.pageIndex || null, size: event.pageSize === DEFAULT_SIZE ? null : event.pageSize });
  }

  protected open(employee: Employee): void {
    this.router.navigate(['/employees', employee.id]);
  }

  private navigate(params: Params, replaceUrl = false): void {
    this.router.navigate([], { relativeTo: this.route, queryParams: params, queryParamsHandling: 'merge', replaceUrl });
  }
}
