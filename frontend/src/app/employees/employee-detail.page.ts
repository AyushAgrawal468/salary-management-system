import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { firstValueFrom, forkJoin, map } from 'rxjs';
import { EmployeeApi } from './employee-api';
import { EmployeeForm } from './employee-form';
import { Employee, NewEmployee, SalaryEntry } from './employee.model';
import { AuthService } from '../core/auth.service';
import { errorMessage, fieldErrors } from '../core/api-error';
import { countryName, formatDate, formatMoney } from '../core/format';

type Mode = 'view' | 'edit' | 'raise' | 'terminate';

/** One employee: profile, pay history, and (for HR) edit, raise and termination. */
@Component({
  selector: 'app-employee-detail',
  imports: [
    ReactiveFormsModule, RouterLink, EmployeeForm, MatButtonModule, MatCardModule, MatFormFieldModule, MatIconModule,
    MatInputModule, MatProgressBarModule,
  ],
  template: `
    <a mat-button routerLink="/employees" class="back"><mat-icon>arrow_back</mat-icon> Employees</a>

    @if (busy()) {
      <mat-progress-bar mode="indeterminate" />
    }
    @if (error()) {
      <div class="alert" role="alert">
        <span>{{ error() }}</span>
        @if (conflict()) {
          <button mat-stroked-button (click)="reload()">Reload</button>
        }
      </div>
    }

    @if (employee(); as e) {
      <header class="header">
        <div>
          <h1>{{ e.fullName }}</h1>
          <p class="subtitle">
            {{ e.employeeCode }} · {{ e.jobTitle }}
            <span class="status" [class.terminated]="e.status === 'TERMINATED'">
              {{ e.status === 'TERMINATED' ? 'Terminated' : 'Active' }}
            </span>
          </p>
        </div>
        @if (auth.canEdit() && mode() === 'view') {
          <div class="actions">
            <button mat-stroked-button (click)="setMode('edit')"><mat-icon>edit</mat-icon> Edit</button>
            @if (e.status === 'ACTIVE') {
              <button mat-stroked-button (click)="startRaise()"><mat-icon>trending_up</mat-icon> Add raise</button>
              <button mat-stroked-button class="danger" (click)="setMode('terminate')">Terminate</button>
            }
          </div>
        }
      </header>

      @if (mode() === 'edit') {
        <mat-card appearance="outlined" class="panel">
          <mat-card-header><mat-card-title>Edit details</mat-card-title></mat-card-header>
          <mat-card-content>
            <app-employee-form [value]="e" [busy]="busy()" [serverErrors]="fieldErrors()"
                               (saved)="save($event)" (cancelled)="setMode('view')" />
          </mat-card-content>
        </mat-card>
      }

      @if (mode() === 'raise') {
        <mat-card appearance="outlined" class="panel">
          <mat-card-header><mat-card-title>Add raise</mat-card-title></mat-card-header>
          <mat-card-content>
            <form [formGroup]="raiseForm" (ngSubmit)="saveRaise()" class="inline-form">
              <mat-form-field>
                <mat-label>New annual salary</mat-label>
                <input matInput type="number" name="amount" formControlName="amount" min="0.01" step="0.01" required />
              </mat-form-field>
              <mat-form-field class="narrow">
                <mat-label>Currency</mat-label>
                <input matInput name="currency" formControlName="currency" maxlength="3" required />
              </mat-form-field>
              <mat-form-field>
                <mat-label>Effective from</mat-label>
                <input matInput type="date" name="effectiveDate" formControlName="effectiveDate" required />
              </mat-form-field>
              <div class="form-actions">
                <button mat-button type="button" (click)="setMode('view')">Cancel</button>
                <button mat-flat-button type="submit" [disabled]="busy()">Save raise</button>
              </div>
            </form>
          </mat-card-content>
        </mat-card>
      }

      @if (mode() === 'terminate') {
        <mat-card appearance="outlined" class="panel">
          <mat-card-header>
            <mat-card-title>Terminate {{ e.fullName }}</mat-card-title>
            <mat-card-subtitle>The record and pay history are kept; this cannot be undone here.</mat-card-subtitle>
          </mat-card-header>
          <mat-card-content>
            <form [formGroup]="terminateForm" (ngSubmit)="terminate()" class="inline-form">
              <mat-form-field>
                <mat-label>Last working day</mat-label>
                <input matInput type="date" name="terminationDate" formControlName="terminationDate" required />
              </mat-form-field>
              <div class="form-actions">
                <button mat-button type="button" (click)="setMode('view')">Cancel</button>
                <button mat-flat-button type="submit" class="danger-fill" [disabled]="busy()">Confirm termination</button>
              </div>
            </form>
          </mat-card-content>
        </mat-card>
      }

      <div class="columns">
        <mat-card appearance="outlined">
          <mat-card-header><mat-card-title>Profile</mat-card-title></mat-card-header>
          <mat-card-content>
            <dl>
              <dt>Department</dt><dd>{{ e.department }}</dd>
              <dt>Country</dt><dd>{{ countryName(e.country) }}</dd>
              <dt>Work email</dt><dd><a [href]="'mailto:' + e.workEmail">{{ e.workEmail }}</a></dd>
              <dt>Phone</dt><dd>{{ e.phone || '—' }}</dd>
              <dt>Hired</dt><dd>{{ formatDate(e.hireDate) }}</dd>
              @if (e.terminationDate) {
                <dt>Last day</dt><dd>{{ formatDate(e.terminationDate) }}</dd>
              }
            </dl>
          </mat-card-content>
        </mat-card>

        <mat-card appearance="outlined">
          <mat-card-header>
            <mat-card-title>Pay</mat-card-title>
            <mat-card-subtitle>Annual base salary</mat-card-subtitle>
          </mat-card-header>
          <mat-card-content>
            @if (e.currentSalary; as s) {
              <p class="current">{{ formatMoney(s.amount, s.currency) }}</p>
              <p class="hint">Since {{ formatDate(s.effectiveDate) }}</p>
            } @else {
              <p class="hint">No salary recorded yet.</p>
            }
            @if (history().length) {
              <table class="history">
                <thead><tr><th>Effective</th><th class="num">Salary</th><th class="num">Change</th></tr></thead>
                <tbody>
                  @for (row of history(); track row.id) {
                    <tr>
                      <td>{{ formatDate(row.effectiveDate) }}</td>
                      <td class="num">{{ formatMoney(row.amount, row.currency) }}</td>
                      <td class="num">{{ row.change }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            }
          </mat-card-content>
        </mat-card>
      </div>
    }
  `,
  styles: `
    .back { margin: -8px 0 8px -8px; }
    .header { display: flex; flex-wrap: wrap; justify-content: space-between; gap: 16px; margin-bottom: 16px; }
    h1 { font: var(--mat-sys-headline-small); margin: 0; }
    .subtitle { margin: 4px 0 0; color: var(--mat-sys-on-surface-variant); display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
    .actions { display: flex; gap: 8px; flex-wrap: wrap; align-items: flex-start; }
    .danger { color: var(--mat-sys-error); }
    .danger-fill { background: var(--mat-sys-error); color: var(--mat-sys-on-error); }
    .status {
      padding: 2px 10px; border-radius: 999px; font: var(--mat-sys-label-medium);
      background: var(--mat-sys-secondary-container); color: var(--mat-sys-on-secondary-container);
    }
    .status.terminated { background: var(--mat-sys-error-container); color: var(--mat-sys-on-error-container); }
    .alert {
      display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 12px 16px; margin-bottom: 16px;
      border-radius: var(--mat-sys-corner-small); background: var(--mat-sys-error-container); color: var(--mat-sys-on-error-container);
    }
    .panel { margin-bottom: 16px; }
    .panel mat-card-content { padding-top: 16px; }
    .inline-form { display: flex; flex-wrap: wrap; gap: 4px 16px; align-items: flex-start; }
    .inline-form mat-form-field { flex: 1 1 200px; }
    .inline-form .narrow { flex: 0 1 120px; }
    .form-actions { flex-basis: 100%; display: flex; justify-content: flex-end; gap: 8px; }
    .columns { display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 16px; }
    dl { display: grid; grid-template-columns: max-content 1fr; gap: 8px 24px; margin: 8px 0 0; }
    dt { color: var(--mat-sys-on-surface-variant); }
    dd { margin: 0; overflow-wrap: anywhere; }
    .current { font: var(--mat-sys-headline-medium); margin: 8px 0 0; font-variant-numeric: tabular-nums; }
    .hint { color: var(--mat-sys-on-surface-variant); margin: 0 0 16px; }
    .history { width: 100%; border-collapse: collapse; }
    .history th { text-align: left; font: var(--mat-sys-label-medium); color: var(--mat-sys-on-surface-variant); padding: 6px 0; }
    .history td { padding: 6px 0; border-top: 1px solid var(--mat-sys-outline-variant); }
    .num { text-align: right !important; font-variant-numeric: tabular-nums; }
  `,
})
export class EmployeeDetailPage {
  private readonly api = inject(EmployeeApi);
  private readonly fb = inject(NonNullableFormBuilder);
  protected readonly auth = inject(AuthService);
  protected readonly countryName = countryName;
  protected readonly formatDate = formatDate;
  protected readonly formatMoney = formatMoney;

  private id = '';
  protected readonly employee = signal<Employee | null>(null);
  private readonly salaries = signal<SalaryEntry[]>([]);
  protected readonly mode = signal<Mode>('view');
  protected readonly busy = signal(false);
  protected readonly error = signal('');
  protected readonly conflict = signal(false);
  protected readonly fieldErrors = signal<Record<string, string>>({});

  /** Newest first, each with its change against the entry before it. */
  protected readonly history = computed(() => {
    const entries = this.salaries();
    return entries.map((entry, i) => {
      const previous = entries[i + 1];
      const change = previous && previous.currency === entry.currency
        ? `${entry.amount >= previous.amount ? '+' : ''}${((entry.amount / previous.amount - 1) * 100).toFixed(1)}%`
        : '—';
      return { ...entry, change };
    });
  });

  protected readonly raiseForm = this.fb.group({
    amount: [null as number | null, [Validators.required, Validators.min(0.01)]],
    currency: ['', [Validators.required, Validators.pattern(/^[A-Z]{3}$/)]],
    effectiveDate: ['', Validators.required],
  });
  protected readonly terminateForm = this.fb.group({ terminationDate: ['', Validators.required] });

  constructor() {
    inject(ActivatedRoute).paramMap.pipe(map(p => p.get('id')!), takeUntilDestroyed()).subscribe(id => {
      this.id = id;
      this.reload();
    });
  }

  protected async reload(): Promise<void> {
    this.setMode('view');
    await this.run(async () => {
      const { employee, salaries } = await firstValueFrom(
        forkJoin({ employee: this.api.get(this.id), salaries: this.api.salaries(this.id) }),
      );
      this.employee.set(employee);
      this.salaries.set(salaries);
    }, 'Could not load this employee.');
  }

  protected setMode(mode: Mode): void {
    this.mode.set(mode);
    this.error.set('');
    this.conflict.set(false);
    this.fieldErrors.set({});
  }

  protected startRaise(): void {
    const e = this.employee()!;
    this.raiseForm.reset({ amount: null, currency: e.currentSalary?.currency ?? this.salaries()[0]?.currency ?? '', effectiveDate: '' });
    this.setMode('raise');
  }

  protected async save(details: NewEmployee): Promise<void> {
    const { employeeCode: _, ...editable } = details;
    await this.run(async () => {
      this.employee.set(await firstValueFrom(this.api.update(this.id, { ...editable, version: this.employee()!.version })));
      this.setMode('view');
    }, 'Could not save the changes.');
  }

  protected async saveRaise(): Promise<void> {
    if (this.raiseForm.invalid) {
      this.raiseForm.markAllAsTouched();
      return;
    }
    const { amount, currency, effectiveDate } = this.raiseForm.getRawValue();
    const saved = await this.run(
      () => firstValueFrom(this.api.addSalary(this.id, { amount: amount!, currency, effectiveDate })),
      'Could not save the raise.',
    );
    // A raise changes the current pay and bumps the employee's version: reload both.
    if (saved) await this.reload();
  }

  protected async terminate(): Promise<void> {
    if (this.terminateForm.invalid) {
      this.terminateForm.markAllAsTouched();
      return;
    }
    const { terminationDate } = this.terminateForm.getRawValue();
    await this.run(async () => {
      this.employee.set(await firstValueFrom(
        this.api.terminate(this.id, { terminationDate, version: this.employee()!.version }),
      ));
      this.setMode('view');
    }, 'Could not terminate this employee.');
  }

  /** Runs a request with the busy bar, turning failures into messages. Resolves true on success. */
  private async run(work: () => Promise<unknown>, fallback: string): Promise<boolean> {
    this.busy.set(true);
    this.error.set('');
    try {
      await work();
      return true;
    } catch (e) {
      this.error.set(errorMessage(e, fallback));
      this.conflict.set(e instanceof HttpErrorResponse && e.status === 409);
      this.fieldErrors.set(fieldErrors(e));
      return false;
    } finally {
      this.busy.set(false);
    }
  }
}
