import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ArtikelService } from '../../services/artikel.service';
import { Article, Statistik, SearchResult } from '../../models/artikel.model';

@Component({
  selector: 'app-startseite',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <div class="card welcome">
      <h1>Willkommen in der Wissensdatenbank</h1>
      <p>Finden, teilen und nutzen Sie das Wissen Ihrer Organisation. Durchsuchen Sie Artikel hierarchisch,
         nutzen Sie die intelligente Suche oder stellen Sie Fragen im Wissenschat.</p>
    </div>

    <!-- Quick search -->
    <div class="card quick-search">
      <div class="search-row">
        <input type="text" [(ngModel)]="searchQuery" (keyup.enter)="quickSearch()"
               placeholder="Wonach suchen Sie?" class="search-input">
        <button class="btn btn-primary" (click)="quickSearch()" [disabled]="!searchQuery.trim()">Suchen</button>
      </div>
      <div *ngIf="searchResults.length > 0" class="quick-results">
        <a *ngFor="let r of searchResults" [routerLink]="'/artikel/' + r.id" class="quick-result">
          <span class="result-title">{{ r.title }}</span>
          <span class="result-breadcrumb" *ngIf="r.breadcrumb && r.breadcrumb.length > 1">
            {{ r.breadcrumb.join(' > ') }}
          </span>
        </a>
        <a routerLink="/suche" [queryParams]="{q: searchQuery}" class="more-link">Alle Ergebnisse anzeigen &rarr;</a>
      </div>
    </div>

    <div class="stats-grid">
      <div class="card stat-card">
        <div class="stat-value">{{ stats.veroeffentlicht }}</div>
        <div class="stat-label">Veroeffentlicht</div>
      </div>
      <div class="card stat-card">
        <div class="stat-value">{{ stats.entwuerfe }}</div>
        <div class="stat-label">Entwuerfe</div>
      </div>
      <div class="card stat-card">
        <div class="stat-value">{{ stats.kategorien }}</div>
        <div class="stat-label">Kategorien</div>
      </div>
      <div class="card stat-card action-card">
        <a routerLink="/chat" class="btn btn-primary" style="width:100%;margin-bottom:0.5rem">Wissenschat starten</a>
        <a routerLink="/artikel/neu" class="btn btn-secondary" style="width:100%">+ Neuer Artikel</a>
      </div>
    </div>

    <div class="section-grid">
      <div>
        <h2 class="section-title">Neueste Beitraege</h2>
        <div class="article-list">
          <a *ngFor="let a of newest" [routerLink]="'/artikel/' + a.id" class="card article-card">
            <div class="article-header">
              <div>
                <h3>{{ a.title }}</h3>
                <div class="article-path" *ngIf="a.breadcrumb && a.breadcrumb.length > 1">
                  <span *ngFor="let bc of a.breadcrumb; let last = last; let first = first">
                    <span *ngIf="!last && !first">{{ bc.title }} &rsaquo; </span>
                  </span>
                </div>
                <p class="article-summary">{{ a.summary || (a.content | slice:0:120) }}{{ !a.summary && a.content.length > 120 ? '...' : '' }}</p>
              </div>
              <span *ngIf="a.category" class="tag">{{ a.category.name }}</span>
            </div>
            <div class="article-meta">
              <span class="status-badge" [class]="a.status.toLowerCase()">{{ statusLabel(a.status) }}</span>
              <span>{{ formatDate(a.createdAt) }}</span>
              <span>{{ a.viewCount }} Aufrufe</span>
              <span *ngIf="a.childCount > 0" class="children-info">{{ a.childCount }} Unterartikel</span>
            </div>
          </a>
          <div *ngIf="newest.length === 0" class="empty-state">Noch keine Artikel vorhanden.</div>
        </div>
      </div>
      <div>
        <h2 class="section-title">Beliebteste Beitraege</h2>
        <div class="article-list">
          <a *ngFor="let a of popular" [routerLink]="'/artikel/' + a.id" class="card article-card compact">
            <h3>{{ a.title }}</h3>
            <div class="article-meta">
              <span *ngIf="a.category" class="tag small">{{ a.category.name }}</span>
              <span>{{ a.viewCount }} Aufrufe</span>
              <span *ngIf="a.averageRating > 0">{{ a.averageRating.toFixed(1) }} / 5</span>
            </div>
          </a>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .welcome h1 { font-size: 1.375rem; font-weight: 600; color: #006EC7; margin-bottom: 0.375rem; }
    .welcome p { color: #6b7280; font-size: 0.9375rem; line-height: 1.6; }
    .welcome { margin-bottom: 1.5rem; }
    .quick-search { margin-bottom: 1.5rem; }
    .search-row { display: flex; gap: 0.75rem; }
    .search-input { flex: 1; font-size: 1rem; padding: 0.5rem 0.75rem; }
    .quick-results { margin-top: 0.75rem; border-top: 1px solid #e5e7eb; padding-top: 0.75rem; }
    .quick-result { display: block; padding: 0.5rem 0.75rem; border-radius: 0.375rem; text-decoration: none; color: #374151; font-size: 0.875rem; }
    .quick-result:hover { background: #f9fafb; }
    .result-title { font-weight: 500; display: block; }
    .result-breadcrumb { font-size: 0.6875rem; color: #9ca3af; }
    .more-link { display: block; text-align: center; font-size: 0.8125rem; color: #006EC7; text-decoration: none; padding-top: 0.5rem; }
    .more-link:hover { text-decoration: underline; }
    .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 1rem; margin-bottom: 2rem; }
    .stat-card { text-align: center; padding: 1.25rem; }
    .stat-value { font-size: 2rem; font-weight: 700; color: #006EC7; }
    .stat-label { font-size: 0.8125rem; color: #6b7280; }
    .action-card { display: flex; flex-direction: column; justify-content: center; }
    .section-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 2rem; }
    @media (max-width: 768px) { .section-grid { grid-template-columns: 1fr; } }
    .section-title { font-size: 1.125rem; font-weight: 600; margin-bottom: 1rem; }
    .article-list { display: flex; flex-direction: column; gap: 0.75rem; }
    .article-card { text-decoration: none; color: inherit; transition: box-shadow 0.15s; cursor: pointer; }
    .article-card:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.12); }
    .article-card h3 { font-size: 0.9375rem; font-weight: 600; margin-bottom: 0.125rem; }
    .article-path { font-size: 0.6875rem; color: #9ca3af; margin-bottom: 0.25rem; }
    .article-summary { font-size: 0.8125rem; color: #6b7280; line-height: 1.5; }
    .article-header { display: flex; justify-content: space-between; align-items: start; gap: 1rem; }
    .article-meta { display: flex; gap: 0.75rem; font-size: 0.75rem; color: #9ca3af; margin-top: 0.5rem; align-items: center; }
    .article-card.compact { padding: 1rem; }
    .tag { padding: 0.2rem 0.6rem; background: #dbeafe; color: #1e40af; font-size: 0.6875rem; font-weight: 500; border-radius: 1rem; white-space: nowrap; }
    .tag.small { font-size: 0.625rem; padding: 0.15rem 0.5rem; }
    .status-badge { padding: 0.15rem 0.5rem; border-radius: 0.25rem; font-size: 0.6875rem; font-weight: 600; text-transform: uppercase; }
    .status-badge.published { background: #dcfce7; color: #166534; }
    .status-badge.draft { background: #fef3c7; color: #92400e; }
    .status-badge.archived { background: #f3f4f6; color: #6b7280; }
    .children-info { font-size: 0.625rem; padding: 0.1rem 0.375rem; background: #f0f9ff; color: #0369a1; border-radius: 0.25rem; }
    .empty-state { text-align: center; padding: 2rem; color: #9ca3af; font-size: 0.875rem; }
  `]
})
export class StartseiteComponent implements OnInit {
  private svc = inject(ArtikelService);
  stats: Statistik = { gesamt: 0, veroeffentlicht: 0, entwuerfe: 0, kategorien: 0 };
  newest: Article[] = [];
  popular: Article[] = [];
  searchQuery = '';
  searchResults: SearchResult[] = [];

  ngOnInit(): void {
    this.svc.getStatistik().subscribe({ next: s => this.stats = s });
    this.svc.getNewest(5).subscribe({ next: a => this.newest = a });
    this.svc.getPopular(5).subscribe({ next: a => this.popular = a });
  }

  quickSearch(): void {
    if (!this.searchQuery.trim()) return;
    this.svc.search(this.searchQuery, 'HYBRID', 5).subscribe({
      next: results => this.searchResults = results,
    });
  }

  formatDate(d: string): string {
    return d ? new Date(d).toLocaleDateString('de-DE', { day: '2-digit', month: '2-digit', year: 'numeric' }) : '';
  }

  statusLabel(s: string): string {
    return s === 'PUBLISHED' ? 'Veroeffentlicht' : s === 'DRAFT' ? 'Entwurf' : 'Archiviert';
  }
}
