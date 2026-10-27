import { inject } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';
import { from, switchMap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const isApiRequest = req.url.startsWith(environment.apiBaseUrl);

  if (!isApiRequest || req.headers.has('Authorization')) {
    return next(req);
  }

  return from(auth.getValidToken()).pipe(
    switchMap((token) => {
      if (!token) {
        return throwError(() => new Error('Sign in is required before requesting customer data.'));
      }

      return next(
        req.clone({
          setHeaders: {
            Authorization: `Bearer ${token}`,
          },
        }),
      );
    }),
  );
};
