import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatToolbarModule } from '@angular/material/toolbar';
import { AuthService, Role } from '../core/auth.service';

const ROLE_LABELS: Record<Role, string> = { ADMIN: 'Administrator', HR_MANAGER: 'HR manager', VIEWER: 'Viewer' };

/** Frame for every signed-in page: top bar with navigation and the account menu. */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatButtonModule, MatIconModule, MatMenuModule, MatToolbarModule],
  template: `
    <mat-toolbar class="app-toolbar">
      <a routerLink="/" class="brand">Salary Management</a>
      <nav aria-label="Main">
        <a mat-button routerLink="/employees" routerLinkActive="active-link">Employees</a>
      </nav>
      <span class="spacer"></span>
      <button mat-button [matMenuTriggerFor]="account" aria-label="Account menu">
        <mat-icon>account_circle</mat-icon>
        <span class="username">{{ auth.user()?.username }}</span>
      </button>
      <mat-menu #account="matMenu" xPosition="before">
        <p class="menu-role">Signed in as {{ roleLabel() }}</p>
        <button mat-menu-item (click)="signOut()">
          <mat-icon>logout</mat-icon>
          <span>Sign out</span>
        </button>
      </mat-menu>
    </mat-toolbar>
    <div class="app-content">
      <router-outlet />
    </div>
  `,
  styles: `
    .app-toolbar {
      position: sticky;
      top: 0;
      z-index: 2;
      gap: 8px;
      border-bottom: 1px solid var(--mat-sys-outline-variant);
    }
    .brand {
      color: inherit;
      text-decoration: none;
      font: var(--mat-sys-title-large);
      margin-right: 16px;
    }
    .spacer { flex: 1; }
    .active-link { background: var(--mat-sys-secondary-container); }
    .menu-role {
      margin: 0;
      padding: 8px 16px;
      font: var(--mat-sys-label-medium);
      color: var(--mat-sys-on-surface-variant);
    }
    .app-content {
      max-width: 1280px;
      margin: 0 auto;
      padding: 24px 16px;
    }
    @media (max-width: 600px) {
      .brand { font: var(--mat-sys-title-medium); margin-right: 0; }
      .username { display: none; }
    }
  `,
})
export class Shell {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly roleLabel = computed(() => {
    const role = this.auth.user()?.role;
    return role ? ROLE_LABELS[role] : '';
  });

  async signOut(): Promise<void> {
    await this.auth.logout();
    await this.router.navigateByUrl('/login');
  }
}
