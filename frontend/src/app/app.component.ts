import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <header>
      <div class="header-inner">
        <a routerLink="/" class="logo">
          <span class="logo-icon">WM</span>
          <span class="logo-text">Wissensmanagement</span>
        </a>
        <nav>
          <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{exact: true}">Startseite</a>
          <a routerLink="/artikel" routerLinkActive="active">Wissensdatenbank</a>
        </nav>
      </div>
    </header>
    <main class="container">
      <router-outlet></router-outlet>
    </main>
  `,
  styles: [`
    header {
      background: linear-gradient(135deg, #006EC7 0%, #004a8a 100%);
      color: #fff;
      padding: 0 1rem;
    }
    .header-inner {
      max-width: 960px;
      margin: 0 auto;
      display: flex;
      align-items: center;
      gap: 2rem;
      height: 56px;
    }
    .logo {
      display: flex;
      align-items: center;
      gap: 0.625rem;
      color: #fff;
      text-decoration: none;
      font-weight: 600;
      font-size: 1.05rem;
    }
    .logo-icon {
      width: 32px;
      height: 32px;
      background: rgba(255,255,255,0.2);
      border-radius: 0.5rem;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 0.75rem;
      font-weight: 700;
    }
    nav { display: flex; gap: 0.25rem; }
    nav a {
      color: rgba(255,255,255,0.8);
      text-decoration: none;
      padding: 0.375rem 0.75rem;
      border-radius: 0.375rem;
      font-size: 0.875rem;
      transition: all 0.15s;
    }
    nav a:hover { background: rgba(255,255,255,0.15); color: #fff; text-decoration: none; }
    nav a.active { background: rgba(255,255,255,0.2); color: #fff; }
    main { padding: 1.5rem 1rem; }
  `]
})
export class AppComponent {}
