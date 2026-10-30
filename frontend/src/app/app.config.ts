import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter, withComponentInputBinding } from '@angular/router';

import { routes } from './app.routes';
import { authInterceptor } from './core/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding()),
    // HttpClient's built-in XSRF support already matches the API: it copies the XSRF-TOKEN
    // cookie into an X-XSRF-TOKEN header on every same-origin write.
    provideHttpClient(withInterceptors([authInterceptor])),
  ],
};
