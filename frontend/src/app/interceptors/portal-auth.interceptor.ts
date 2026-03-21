import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Liest das Portal-JWT aus dem localStorage und haengt es an alle API-Requests an.
 * Das Token wird vom PortalCore unter 'portal_token' gespeichert und ist
 * im iframe dank same-origin zugaenglich.
 */
export const portalAuthInterceptor: HttpInterceptorFn = (req, next) => {
  // Nur API-Requests authentifizieren
  if (!req.url.includes('api/')) {
    return next(req);
  }

  const token = localStorage.getItem('portal_token');
  if (token) {
    const authReq = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
    return next(authReq);
  }

  return next(req);
};
