import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./pages/startseite/startseite.component').then(m => m.StartseiteComponent) },
  { path: 'artikel', loadComponent: () => import('./pages/artikel/artikel-list.component').then(m => m.ArtikelListComponent) },
  { path: 'artikel/neu', loadComponent: () => import('./pages/artikel/artikel-form.component').then(m => m.ArtikelFormComponent) },
  { path: 'artikel/:id', loadComponent: () => import('./pages/artikel/artikel-detail.component').then(m => m.ArtikelDetailComponent) },
  { path: 'artikel/:id/bearbeiten', loadComponent: () => import('./pages/artikel/artikel-form.component').then(m => m.ArtikelFormComponent) },
  { path: 'suche', loadComponent: () => import('./pages/suche/suche.component').then(m => m.SucheComponent) },
  { path: 'chat', loadComponent: () => import('./pages/chat/chat.component').then(m => m.ChatComponent) },
  { path: 'kategorien', loadComponent: () => import('./pages/kategorien/kategorien.component').then(m => m.KategorienComponent) },
  { path: '**', redirectTo: '' },
];
