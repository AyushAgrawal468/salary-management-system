import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { Shell } from './layout/shell';

export const routes: Routes = [
  { path: 'login', title: 'Sign in', loadComponent: () => import('./login/login.page').then(m => m.LoginPage) },
  {
    path: 'change-password',
    title: 'Set your password',
    canActivate: [authGuard],
    loadComponent: () => import('./account/change-password.page').then(m => m.ChangePasswordPage),
  },
  {
    path: '',
    title: 'Salary Management',
    component: Shell,
    canActivate: [authGuard],
    children: [],
  },
  { path: '**', redirectTo: '' },
];
