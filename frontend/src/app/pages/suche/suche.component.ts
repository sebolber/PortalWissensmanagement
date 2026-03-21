import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ArtikelService } from '../../services/artikel.service';
import { SearchResult } from '../../models/artikel.model';

@Component({
  selector: 'app-suche',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="search-page">
      <div class="search-header">
        <h1>Wissenssuche</h1>
        <p>Durchsuchen Sie die gesamte Wissensdatenbank</p>
      </div>

      <div class="card search-box">
        <div class="search-input-row">
          <input type="text" [(ngModel)]="query" (keyup.enter)="doSearch()"
                 placeholder="Wonach suchen Sie? Beschreiben Sie Ihr Anliegen..." class="search-input">
          <button class="btn btn-primary" (click)="doSearch()" [disabled]="!query.trim() || searching">
            {{ searching ? 'Suche...' : 'Suchen' }}
          </button>
        </div>
        <div class="search-options">
          <label class="mode-option" *ngFor="let m of searchModes">
            <input type="radio" name="searchMode" [value]="m.value" [(ngModel)]="searchMode">
            <span class="mode-label">{{ m.label }}</span>
            <span class="mode-desc">{{ m.description }}</span>
          </label>
        </div>
      </div>

      <div class="results-info" *ngIf="searched && !searching">
        {{ results.length }} Ergebnis{{ results.length !== 1 ? 'se' : '' }} gefunden
        <span *ngIf="query">&ndash; &laquo;{{ query }}&raquo;</span>
        <span class="mode-tag">{{ searchModeLabel() }}</span>
      </div>

      <div class="results-list">
        <a *ngFor="let r of results; let i = index" [routerLink]="'/artikel/' + r.id" class="card result-card">
          <div class="result-header">
            <div class="result-rank">#{{ i + 1 }}</div>
            <div class="result-main">
              <h3>{{ r.title }}</h3>
              <div class="result-breadcrumb" *ngIf="r.breadcrumb && r.breadcrumb.length > 1">
                <span *ngFor="let b of r.breadcrumb; let last = last">
                  {{ b }}<span *ngIf="!last" class="breadcrumb-sep"> &rsaquo; </span>
                </span>
              </div>
            </div>
            <div class="result-meta">
              <span class="relevance-badge" [class]="getRelevanceClass(r.relevanceScore)">
                {{ (r.relevanceScore * 100).toFixed(0) }}%
              </span>
              <span class="type-badge" [class]="r.searchType.toLowerCase()">{{ searchTypeLabel(r.searchType) }}</span>
            </div>
          </div>
          <p class="result-snippet">{{ r.snippet }}</p>
          <div class="result-footer">
            <span class="status-badge" [class]="r.status.toLowerCase()">{{ statusLabel(r.status) }}</span>
            <span *ngIf="r.depth > 0" class="depth-info">Ebene {{ r.depth + 1 }}</span>
          </div>
        </a>
      </div>

      <div *ngIf="searched && results.length === 0 && !searching" class="empty-state card">
        <h3>Keine Ergebnisse</h3>
        <p>Versuchen Sie andere Suchbegriffe oder eine allgemeinere Formulierung.</p>
        <div class="empty-suggestions">
          <p *ngIf="searchMode !== 'HYBRID'">
            Tipp: Wechseln Sie in den Modus <strong>Hybrid</strong> fuer breitere Ergebnisse.
          </p>
          <p>Sie koennen auch den <a routerLink="/chat">Wissenschat</a> nutzen, um Fragen in natuerlicher Sprache zu stellen.</p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .search-page { max-width: 800px; margin: 0 auto; }
    .search-header { margin-bottom: 1.5rem; }
    .search-header h1 { font-size: 1.375rem; font-weight: 600; margin-bottom: 0.25rem; }
    .search-header p { color: #6b7280; font-size: 0.875rem; }
    .search-box { margin-bottom: 1.5rem; }
    .search-input-row { display: flex; gap: 0.75rem; }
    .search-input { flex: 1; font-size: 1rem; padding: 0.625rem 0.75rem; }
    .search-options { display: flex; gap: 1rem; margin-top: 0.75rem; flex-wrap: wrap; }
    .mode-option { display: flex; align-items: baseline; gap: 0.375rem; cursor: pointer; font-size: 0.8125rem; }
    .mode-option input[type="radio"] { margin: 0; }
    .mode-label { font-weight: 500; }
    .mode-desc { color: #9ca3af; font-size: 0.75rem; }
    .results-info { font-size: 0.8125rem; color: #6b7280; margin-bottom: 0.75rem; display: flex; align-items: center; gap: 0.5rem; flex-wrap: wrap; }
    .mode-tag { font-size: 0.625rem; padding: 0.1rem 0.375rem; background: #eff6ff; color: #006EC7; border-radius: 0.25rem; text-transform: uppercase; }
    .results-list { display: flex; flex-direction: column; gap: 0.75rem; }
    .result-card { text-decoration: none; color: inherit; transition: box-shadow 0.15s; cursor: pointer; }
    .result-card:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.12); }
    .result-header { display: flex; align-items: start; gap: 0.75rem; margin-bottom: 0.5rem; }
    .result-rank { font-size: 0.75rem; color: #9ca3af; font-weight: 600; padding-top: 0.125rem; min-width: 24px; }
    .result-main { flex: 1; min-width: 0; }
    .result-main h3 { font-size: 0.9375rem; font-weight: 600; margin-bottom: 0.125rem; }
    .result-breadcrumb { font-size: 0.6875rem; color: #9ca3af; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .breadcrumb-sep { color: #d1d5db; }
    .result-meta { display: flex; flex-direction: column; gap: 0.25rem; align-items: flex-end; flex-shrink: 0; }
    .relevance-badge { font-size: 0.6875rem; font-weight: 600; padding: 0.1rem 0.4rem; border-radius: 0.25rem; }
    .relevance-badge.high { background: #dcfce7; color: #166534; }
    .relevance-badge.medium { background: #fef3c7; color: #92400e; }
    .relevance-badge.low { background: #f3f4f6; color: #6b7280; }
    .type-badge { font-size: 0.5625rem; padding: 0.1rem 0.375rem; border-radius: 0.25rem; text-transform: uppercase; letter-spacing: 0.05em; }
    .type-badge.fulltext { background: #dbeafe; color: #1e40af; }
    .type-badge.semantic { background: #ede9fe; color: #6d28d9; }
    .type-badge.hybrid { background: #dcfce7; color: #166534; }
    .result-snippet { font-size: 0.8125rem; color: #6b7280; line-height: 1.6; margin-bottom: 0.5rem; }
    .result-footer { display: flex; gap: 0.5rem; align-items: center; }
    .status-badge { padding: 0.1rem 0.4rem; border-radius: 0.25rem; font-size: 0.625rem; font-weight: 600; text-transform: uppercase; }
    .status-badge.published { background: #dcfce7; color: #166534; }
    .status-badge.draft { background: #fef3c7; color: #92400e; }
    .status-badge.archived { background: #f3f4f6; color: #6b7280; }
    .depth-info { font-size: 0.6875rem; color: #9ca3af; }
    .empty-state { text-align: center; padding: 3rem; }
    .empty-state h3 { font-size: 1rem; font-weight: 600; color: #6b7280; margin-bottom: 0.5rem; }
    .empty-state p { font-size: 0.875rem; color: #9ca3af; line-height: 1.6; }
    .empty-suggestions { margin-top: 1rem; }
    .empty-suggestions a { color: #006EC7; text-decoration: none; }
    .empty-suggestions a:hover { text-decoration: underline; }
  `]
})
export class SucheComponent {
  private svc = inject(ArtikelService);

  query = '';
  searchMode = 'HYBRID';
  results: SearchResult[] = [];
  searching = false;
  searched = false;

  searchModes = [
    { value: 'HYBRID', label: 'Hybrid', description: '(empfohlen)' },
    { value: 'FULLTEXT', label: 'Volltext', description: '(exakte Begriffe)' },
    { value: 'SEMANTIC', label: 'Semantisch', description: '(aehnliche Inhalte)' },
  ];

  doSearch(): void {
    if (!this.query.trim()) return;
    this.searching = true;
    this.svc.search(this.query, this.searchMode).subscribe({
      next: results => {
        this.results = results;
        this.searching = false;
        this.searched = true;
      },
      error: () => {
        this.searching = false;
        this.searched = true;
      },
    });
  }

  getRelevanceClass(score: number): string {
    if (score >= 0.7) return 'high';
    if (score >= 0.4) return 'medium';
    return 'low';
  }

  searchModeLabel(): string {
    return this.searchModes.find(m => m.value === this.searchMode)?.label || this.searchMode;
  }

  searchTypeLabel(type: string): string {
    switch (type) {
      case 'FULLTEXT': return 'Volltext';
      case 'SEMANTIC': return 'Semantisch';
      case 'HYBRID': return 'Hybrid';
      default: return type;
    }
  }

  statusLabel(s: string): string {
    return s === 'PUBLISHED' ? 'Veroeffentlicht' : s === 'DRAFT' ? 'Entwurf' : 'Archiviert';
  }
}
