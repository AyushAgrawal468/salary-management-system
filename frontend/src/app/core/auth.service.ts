import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

export type Role = 'ADMIN' | 'HR_MANAGER' | 'VIEWER';

export interface CurrentUser {
  username: string;
  role: Role;
  mustChangePassword: boolean;
}

/** Session state. The session itself is an HttpOnly cookie the browser handles; we only mirror who is signed in. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private sessionChecked = false;

  readonly user = signal<CurrentUser | null>(null);
  readonly canEdit = computed(() => {
    const role = this.user()?.role;
    return role === 'ADMIN' || role === 'HR_MANAGER';
  });

  /** Asks the server once per page load whether a session exists; later calls reuse the answer. */
  async loadSession(): Promise<CurrentUser | null> {
    if (!this.sessionChecked) {
      try {
        this.user.set(await firstValueFrom(this.http.get<CurrentUser>('/api/auth/me')));
      } catch (e) {
        if (!(e instanceof HttpErrorResponse && e.status === 401)) throw e;
        this.user.set(null);
      }
      this.sessionChecked = true;
    }
    return this.user();
  }

  async login(username: string, password: string): Promise<CurrentUser> {
    const user = await firstValueFrom(this.http.post<CurrentUser>('/api/auth/login', { username, password }));
    this.user.set(user);
    this.sessionChecked = true;
    return user;
  }

  async logout(): Promise<void> {
    try {
      await firstValueFrom(this.http.post<void>('/api/auth/logout', null));
    } finally {
      this.clear();
    }
  }

  async changePassword(currentPassword: string, newPassword: string): Promise<void> {
    await firstValueFrom(this.http.post<void>('/api/auth/password', { currentPassword, newPassword }));
    this.user.update(u => (u ? { ...u, mustChangePassword: false } : u));
  }

  /** The server says the session is gone (expired, logged out elsewhere). */
  clear(): void {
    this.user.set(null);
    this.sessionChecked = true;
  }
}
