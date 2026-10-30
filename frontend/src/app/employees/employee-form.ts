import { Component, effect, inject, input, output } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { EmployeeDetails, NewEmployee } from './employee.model';
import { countryName } from '../core/format';

type Field = keyof NewEmployee;

/**
 * Employee fields for both "add" and "edit". Mirrors the API's validation so most mistakes are
 * caught before a round trip; anything the server still rejects is shown next to its field.
 */
@Component({
  selector: 'app-employee-form',
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  template: `
    <form [formGroup]="form" (ngSubmit)="submit()" class="grid">
      @if (withCode()) {
        <mat-form-field>
          <mat-label>Employee code</mat-label>
          <input matInput name="employeeCode" formControlName="employeeCode" required />
          <mat-hint>Your HR system's ID, e.g. EMP-01234. Cannot be changed later.</mat-hint>
          <mat-error>{{ problem('employeeCode') }}</mat-error>
        </mat-form-field>
      }
      <mat-form-field>
        <mat-label>Full name</mat-label>
        <input matInput name="fullName" formControlName="fullName" required />
        <mat-error>{{ problem('fullName') }}</mat-error>
      </mat-form-field>
      <mat-form-field>
        <mat-label>Job title</mat-label>
        <input matInput name="jobTitle" formControlName="jobTitle" required />
        <mat-error>{{ problem('jobTitle') }}</mat-error>
      </mat-form-field>
      <mat-form-field>
        <mat-label>Department</mat-label>
        <input matInput name="department" formControlName="department" required />
        <mat-error>{{ problem('department') }}</mat-error>
      </mat-form-field>
      <mat-form-field>
        <mat-label>Country code</mat-label>
        <input matInput name="country" formControlName="country" maxlength="2" required (input)="upperCaseCountry()" />
        <mat-hint>{{ countryHint() }}</mat-hint>
        <mat-error>{{ problem('country') }}</mat-error>
      </mat-form-field>
      <mat-form-field>
        <mat-label>Work email</mat-label>
        <input matInput type="email" name="workEmail" formControlName="workEmail" required />
        <mat-error>{{ problem('workEmail') }}</mat-error>
      </mat-form-field>
      <mat-form-field>
        <mat-label>Phone (optional)</mat-label>
        <input matInput type="tel" name="phone" formControlName="phone" />
        <mat-error>{{ problem('phone') }}</mat-error>
      </mat-form-field>
      <mat-form-field>
        <mat-label>Hire date</mat-label>
        <input matInput type="date" name="hireDate" formControlName="hireDate" required />
        <mat-error>{{ problem('hireDate') }}</mat-error>
      </mat-form-field>
      <div class="actions">
        <button mat-button type="button" (click)="cancelled.emit()" [disabled]="busy()">Cancel</button>
        <button mat-flat-button type="submit" [disabled]="busy()">{{ submitLabel() }}</button>
      </div>
    </form>
  `,
  styles: `
    .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 4px 16px; }
    .actions { grid-column: 1 / -1; display: flex; justify-content: flex-end; gap: 8px; }
  `,
})
export class EmployeeForm {
  readonly value = input<Partial<NewEmployee>>({});
  readonly withCode = input(false);
  readonly busy = input(false);
  readonly submitLabel = input('Save');
  /** Per-field messages from a 400 response. */
  readonly serverErrors = input<Record<string, string>>({});

  readonly saved = output<NewEmployee>();
  readonly cancelled = output<void>();

  protected readonly form = inject(NonNullableFormBuilder).group({
    employeeCode: ['', [Validators.required, Validators.maxLength(50)]],
    fullName: ['', [Validators.required, Validators.maxLength(255)]],
    jobTitle: ['', [Validators.required, Validators.maxLength(255)]],
    department: ['', [Validators.required, Validators.maxLength(255)]],
    country: ['', [Validators.required, Validators.pattern(/^[A-Z]{2}$/)]],
    workEmail: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    phone: ['', Validators.maxLength(50)],
    hireDate: ['', Validators.required],
  });

  constructor() {
    effect(() => {
      const v = this.value();
      this.form.reset({ ...v, phone: v.phone ?? '' });
      if (!this.withCode()) this.form.controls.employeeCode.disable();
    });
    effect(() => {
      for (const [field, message] of Object.entries(this.serverErrors())) {
        const control = this.form.get(field);
        control?.setErrors({ server: message });
        control?.markAsTouched();
      }
    });
  }

  protected problem(field: Field): string {
    const errors = this.form.controls[field].errors ?? {};
    if (errors['server']) return errors['server'];
    if (errors['required']) return 'Required';
    if (errors['email']) return 'Enter a valid email address';
    if (errors['pattern']) return 'Two-letter ISO code, e.g. IN or US';
    if (errors['maxlength']) return `At most ${errors['maxlength'].requiredLength} characters`;
    return '';
  }

  protected countryHint(): string {
    const code = this.form.controls.country.value;
    return /^[A-Z]{2}$/.test(code) ? countryName(code) : 'Two-letter ISO code, e.g. IN';
  }

  protected upperCaseCountry(): void {
    const control = this.form.controls.country;
    control.setValue(control.value.toUpperCase(), { emitEvent: false });
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const details: EmployeeDetails = {
      fullName: v.fullName.trim(),
      jobTitle: v.jobTitle.trim(),
      department: v.department.trim(),
      country: v.country,
      workEmail: v.workEmail.trim(),
      phone: v.phone.trim() || null,
      hireDate: v.hireDate,
    };
    this.saved.emit({ ...details, employeeCode: v.employeeCode.trim() });
  }
}
