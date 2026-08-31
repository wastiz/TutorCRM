import { HttpInterceptorFn } from '@angular/common/http';

/** Ensures the session cookie rides along with every API call. */
export const credentialsInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req.clone({ withCredentials: true }));
};
