import { Component, inject, signal } from '@angular/core';
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { AuthService } from '../core/auth.service';
import { errorMessage } from '../core/api-error';

const passwordsMatch = (group: AbstractControl): ValidationErrors | null =>
  group.get('newPassword')?.value === group.get('confirm')?.value ? null : { mismatch: true };

@Component({
  selector: 'app-change-password',
  imports: [ReactiveFormsModule, MatButtonModule, MatCardModule, MatFormFieldModule, MatInputModule, MatProgressBarModule],
  template: `
    <main class="auth-page">
      <mat-card appearance="outlined" class="auth-card">
        @if (busy()) {
          <mat-progress-bar mode="indeterminate" />
        }
        <mat-card-header>
          <mat-card-title>Set your password</mat-card-title>
          <mat-card-subtitle>Choose a password only you know before continuing.</mat-card-subtitle>
        </mat-card-header>
        <form [formGroup]="form" (ngSubmit)="submit()">
          <mat-card-content>
            <mat-form-field>
              <mat-label>Current password</mat-label>
              <input matInput type="password" formControlName="currentPassword" autocomplete="current-password" required />
            </mat-form-field>
            <mat-form-field>
              <mat-label>New password</mat-label>
              <input matInput type="password" formControlName="newPassword" autocomplete="new-password" required />
              <mat-hint>At least 12 characters. A short phrase is easier to remember.</mat-hint>
              @if (form.controls.newPassword.hasError('minlength') || form.controls.newPassword.hasError('maxlength')) {
                <mat-error>At least 12 characters, at most 72.</mat-error>
              }
            </mat-form-field>
            <mat-form-field>
              <mat-label>Confirm new password</mat-label>
              <input matInput type="password" formControlName="confirm" autocomplete="new-password" required />
            </mat-form-field>
            @if (form.hasError('mismatch') && form.controls.confirm.touched) {
              <p class="form-error">Passwords do not match.</p>
            }
            @if (error()) {
              <p class="form-error" role="alert">{{ error() }}</p>
            }
          </mat-card-content>
          <mat-card-actions align="end">
            <button mat-flat-button type="submit" [disabled]="busy()">Save password</button>
          </mat-card-actions>
        </form>
      </mat-card>
    </main>
  `,
})
export class ChangePasswordPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly form = inject(NonNullableFormBuilder).group(
    {
      currentPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(12), Validators.maxLength(72)]],
      confirm: ['', Validators.required],
    },
    { validators: passwordsMatch },
  );
  readonly busy = signal(false);
  readonly error = signal('');

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.busy.set(true);
    this.error.set('');
    try {
      const { currentPassword, newPassword } = this.form.getRawValue();
      await this.auth.changePassword(currentPassword, newPassword);
      await this.router.navigateByUrl('/');
    } catch (e) {
      this.error.set(errorMessage(e, 'Could not change the password. Please try again.'));
    } finally {
      this.busy.set(false);
    }
  }
}
