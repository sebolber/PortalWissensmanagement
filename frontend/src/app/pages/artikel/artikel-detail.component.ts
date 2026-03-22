import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ArtikelService } from '../../services/artikel.service';
import { Article, ArticleVersion } from '../../models/artikel.model';
import { marked } from 'marked';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-artikel-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <!-- Breadcrumb -->
    <nav class="breadcrumb" *ngIf="article?.breadcrumb && article!.breadcrumb!.length > 0">
      <a routerLink="/artikel" class="breadcrumb-link">Wissensdatenbank</a>
      <ng-container *ngFor="let bc of article!.breadcrumb; let last = last">
        <span class="breadcrumb-sep">&rsaquo;</span>
        <a *ngIf="!last" [routerLink]="'/artikel/' + bc.id" class="breadcrumb-link">{{ bc.title }}</a>
        <span *ngIf="last" class="breadcrumb-current">{{ bc.title }}</span>
      </ng-container>
    </nav>

    <a *ngIf="!article?.breadcrumb?.length" routerLink="/artikel" class="back-link">&larr; Zurueck zur Wissensdatenbank</a>

    <div *ngIf="article" class="article-layout">
      <!-- Main article content -->
      <div class="article-main">
        <!-- Uebergeordneter Artikel (prominent ueber dem Unterartikel) -->
        <div *ngIf="parentArticle" class="parent-article-banner card">
          <span class="parent-banner-label">Uebergeordneter Artikel:</span>
          <a [routerLink]="'/artikel/' + parentArticle.id" class="parent-banner-link">
            &larr; {{ parentArticle.title }}
          </a>
        </div>

        <div class="card">
          <div class="detail-header">
            <div>
              <h1>{{ article.title }}</h1>
              <div class="meta-row">
                <span class="status-badge" [class]="article.status.toLowerCase()">{{ statusLabel(article.status) }}</span>
                <span>Version {{ article.version }}</span>
                <span>{{ formatDate(article.createdAt) }}</span>
                <span *ngIf="article.updatedAt">Aktualisiert: {{ formatDate(article.updatedAt) }}</span>
                <span>{{ article.viewCount }} Aufrufe</span>
              </div>
            </div>
            <div class="header-badges">
              <span *ngIf="article.category" class="tag">{{ article.category.name }}</span>
              <span *ngIf="article.grouping" class="grouping-tag">{{ article.grouping.name }}</span>
            </div>
          </div>

          <div *ngIf="article.tags.length > 0" class="tag-row">
            <span *ngFor="let t of article.tags" class="meta-tag">{{ t.name }}</span>
          </div>

          <div *ngIf="article.summary" class="summary-box">
            <strong>Zusammenfassung:</strong> {{ article.summary }}
          </div>

          <div class="content-area markdown-body" [innerHTML]="renderedContent"></div>

          <!-- Rating -->
          <div class="rating-section">
            <span class="rating-label">Bewertung:</span>
            <button *ngFor="let star of [1,2,3,4,5]" class="star-btn" [class.active]="star <= userRating"
                    (click)="rate(star)">&#9733;</button>
            <span *ngIf="article.ratingCount > 0" class="rating-info">
              {{ article.averageRating.toFixed(1) }} / 5 ({{ article.ratingCount }} Bewertungen)
            </span>
          </div>

          <div class="action-bar">
            <div class="action-group">
              <a [routerLink]="'/artikel/' + article.id + '/bearbeiten'" class="btn btn-secondary">Bearbeiten</a>
              <button *ngIf="article.status === 'DRAFT'" (click)="onPublish()" class="btn btn-primary">Veroeffentlichen</button>
              <button *ngIf="article.status === 'PUBLISHED'" (click)="onArchive()" class="btn btn-secondary">Archivieren</button>
              <a [routerLink]="'/artikel/neu'" [queryParams]="{parent: article.id}" class="btn btn-secondary">+ Unterartikel</a>
            </div>
            <button (click)="onDelete()" class="btn btn-danger">Loeschen</button>
          </div>
        </div>

        <!-- Children / Sub-articles -->
        <div *ngIf="childArticles.length > 0" class="card" style="margin-top:1rem">
          <h2 class="section-title">Unterartikel ({{ childArticles.length }})</h2>
          <div class="children-list">
            <a *ngFor="let child of childArticles" [routerLink]="'/artikel/' + child.id" class="child-card">
              <div class="child-main">
                <h4>{{ child.title }}</h4>
                <p *ngIf="child.summary">{{ child.summary }}</p>
              </div>
              <div class="child-meta">
                <span class="status-badge small" [class]="child.status.toLowerCase()">{{ statusLabel(child.status) }}</span>
              </div>
            </a>
          </div>
        </div>

        <!-- Version history -->
        <div *ngIf="versions.length > 0" class="card" style="margin-top:1rem">
          <h2 class="section-title">Versionshistorie</h2>
          <div *ngFor="let v of versions" class="version-item">
            <div class="version-header">
              <strong>Version {{ v.version }}</strong>
              <span>{{ formatDate(v.changedAt) }}</span>
            </div>
            <div *ngIf="v.changeNote" class="version-note">{{ v.changeNote }}</div>
          </div>
        </div>
      </div>

      <!-- Sidebar: parent and siblings -->
      <aside class="article-sidebar" *ngIf="article.parentArticleId || childArticles.length > 0">
        <div class="card sidebar-card">
          <h3 class="sidebar-title">Artikelstruktur</h3>

          <div *ngIf="parentArticle" class="parent-section">
            <span class="sidebar-label">Uebergeordnet:</span>
            <a [routerLink]="'/artikel/' + parentArticle.id" class="parent-link">
              &larr; {{ parentArticle.title }}
            </a>
          </div>

          <div *ngIf="childArticles.length > 0" class="children-section">
            <span class="sidebar-label">Unterartikel:</span>
            <div *ngFor="let child of childArticles" class="sidebar-child">
              <a [routerLink]="'/artikel/' + child.id">{{ child.title }}</a>
            </div>
          </div>
        </div>
      </aside>
    </div>

    <div *ngIf="!article && !loading" class="empty-state">Artikel nicht gefunden.</div>
  `,
  styles: [`
    .breadcrumb { display: flex; align-items: center; gap: 0.375rem; margin-bottom: 1rem; font-size: 0.8125rem; flex-wrap: wrap; }
    .breadcrumb-link { color: #6b7280; text-decoration: none; }
    .breadcrumb-link:hover { color: #006EC7; }
    .breadcrumb-sep { color: #d1d5db; }
    .breadcrumb-current { color: #374151; font-weight: 500; }
    .back-link { display: inline-flex; align-items: center; gap: 0.375rem; font-size: 0.875rem; color: #6b7280; margin-bottom: 1rem; text-decoration: none; }
    .back-link:hover { color: #006EC7; }
    .article-layout { display: grid; grid-template-columns: 1fr 240px; gap: 1rem; align-items: start; }
    @media (max-width: 900px) { .article-layout { grid-template-columns: 1fr; } .article-sidebar { order: -1; } }
    .article-main { min-width: 0; }
    .parent-article-banner { display: flex; align-items: center; gap: 0.5rem; padding: 0.625rem 1rem; margin-bottom: 0.75rem; background: #f0f9ff; border-left: 3px solid #006EC7; }
    .parent-banner-label { font-size: 0.75rem; color: #6b7280; text-transform: uppercase; letter-spacing: 0.03em; font-weight: 600; white-space: nowrap; }
    .parent-banner-link { font-size: 0.9375rem; color: #006EC7; text-decoration: none; font-weight: 600; }
    .parent-banner-link:hover { text-decoration: underline; }
    .detail-header { display: flex; justify-content: space-between; align-items: start; gap: 1rem; margin-bottom: 1rem; }
    .detail-header h1 { font-size: 1.375rem; font-weight: 600; margin-bottom: 0.375rem; }
    .meta-row { display: flex; gap: 0.75rem; font-size: 0.8125rem; color: #6b7280; align-items: center; flex-wrap: wrap; }
    .tag { padding: 0.2rem 0.6rem; background: #dbeafe; color: #1e40af; font-size: 0.75rem; font-weight: 500; border-radius: 1rem; }
    .grouping-tag { padding: 0.2rem 0.6rem; background: #fef3c7; color: #92400e; font-size: 0.75rem; font-weight: 500; border-radius: 1rem; }
    .header-badges { display: flex; gap: 0.375rem; flex-wrap: wrap; }
    .tag-row { display: flex; gap: 0.375rem; flex-wrap: wrap; margin-bottom: 1rem; }
    .meta-tag { padding: 0.15rem 0.5rem; background: #f3f4f6; border-radius: 0.25rem; font-size: 0.75rem; color: #6b7280; }
    .summary-box { padding: 0.75rem 1rem; background: #f0f9ff; border-radius: 0.5rem; font-size: 0.875rem; color: #1e40af; margin-bottom: 1.5rem; line-height: 1.6; }

    /* Markdown content area */
    .content-area { font-size: 0.9375rem; line-height: 1.8; color: #374151; margin-bottom: 1.5rem; }
    .content-area :first-child { margin-top: 0; }
    .content-area :last-child { margin-bottom: 0; }

    .status-badge { padding: 0.15rem 0.5rem; border-radius: 0.25rem; font-size: 0.6875rem; font-weight: 600; text-transform: uppercase; }
    .status-badge.small { font-size: 0.5625rem; padding: 0.1rem 0.375rem; }
    .status-badge.published { background: #dcfce7; color: #166534; }
    .status-badge.draft { background: #fef3c7; color: #92400e; }
    .status-badge.archived { background: #f3f4f6; color: #6b7280; }
    .rating-section { display: flex; align-items: center; gap: 0.375rem; margin-bottom: 1.5rem; padding-top: 1rem; border-top: 1px solid #e5e7eb; }
    .rating-label { font-size: 0.875rem; color: #6b7280; margin-right: 0.25rem; }
    .star-btn { border: none; background: none; font-size: 1.25rem; color: #d1d5db; cursor: pointer; padding: 0; }
    .star-btn.active { color: #f59e0b; }
    .star-btn:hover { color: #f59e0b; }
    .rating-info { font-size: 0.75rem; color: #9ca3af; margin-left: 0.5rem; }
    .action-bar { display: flex; justify-content: space-between; padding-top: 1.5rem; border-top: 1px solid #e5e7eb; flex-wrap: wrap; gap: 0.5rem; }
    .action-group { display: flex; gap: 0.75rem; flex-wrap: wrap; }
    .section-title { font-size: 1rem; font-weight: 600; margin-bottom: 1rem; }
    .children-list { display: flex; flex-direction: column; gap: 0.5rem; }
    .child-card { display: flex; justify-content: space-between; align-items: center; padding: 0.75rem; background: #f9fafb; border-radius: 0.5rem; text-decoration: none; color: inherit; transition: background 0.15s; }
    .child-card:hover { background: #eff6ff; }
    .child-card h4 { font-size: 0.875rem; font-weight: 600; margin-bottom: 0.125rem; }
    .child-card p { font-size: 0.75rem; color: #6b7280; }
    .version-item { padding: 0.75rem 0; border-bottom: 1px solid #f3f4f6; }
    .version-item:last-child { border-bottom: none; }
    .version-header { display: flex; justify-content: space-between; font-size: 0.875rem; }
    .version-note { font-size: 0.8125rem; color: #6b7280; margin-top: 0.25rem; }
    .sidebar-card { padding: 1rem; }
    .sidebar-title { font-size: 0.875rem; font-weight: 600; margin-bottom: 0.75rem; }
    .sidebar-label { font-size: 0.6875rem; text-transform: uppercase; letter-spacing: 0.05em; color: #9ca3af; font-weight: 600; display: block; margin-bottom: 0.25rem; margin-top: 0.75rem; }
    .sidebar-label:first-child { margin-top: 0; }
    .parent-link { font-size: 0.8125rem; color: #006EC7; text-decoration: none; display: block; }
    .parent-link:hover { text-decoration: underline; }
    .sidebar-child a { font-size: 0.8125rem; color: #374151; text-decoration: none; display: block; padding: 0.25rem 0; }
    .sidebar-child a:hover { color: #006EC7; }
    .empty-state { text-align: center; padding: 3rem; color: #9ca3af; }
  `]
})
export class ArtikelDetailComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private svc = inject(ArtikelService);
  private routeSub?: Subscription;

  article: Article | null = null;
  parentArticle: Article | null = null;
  childArticles: Article[] = [];
  versions: ArticleVersion[] = [];
  loading = true;
  userRating = 0;
  renderedContent = '';

  ngOnInit(): void {
    this.routeSub = this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.resetState();
        this.loadArticle(id);
      }
    });
  }

  ngOnDestroy(): void {
    this.routeSub?.unsubscribe();
  }

  private resetState(): void {
    this.article = null;
    this.parentArticle = null;
    this.childArticles = [];
    this.versions = [];
    this.loading = true;
    this.userRating = 0;
    this.renderedContent = '';
  }

  private loadArticle(id: string): void {
    this.svc.getById(id).subscribe({
      next: a => {
        this.article = a;
        this.renderedContent = this.renderMarkdown(a.content);
        this.loading = false;

        this.svc.getChildren(id).subscribe({
          next: children => this.childArticles = children,
        });

        if (a.parentArticleId) {
          this.svc.getById(a.parentArticleId, false).subscribe({
            next: parent => this.parentArticle = parent,
          });
        }
      },
      error: () => this.loading = false,
    });
    this.svc.getVersions(id).subscribe({ next: v => this.versions = v });
  }

  private renderMarkdown(content: string): string {
    if (!content) return '';
    // If content looks like HTML (from old rich editor), display as-is
    if (content.trim().startsWith('<') && /<\/?[a-z][\s\S]*>/i.test(content)) {
      return content;
    }
    return marked.parse(content, { async: false }) as string;
  }

  onPublish(): void {
    if (!this.article) return;
    this.svc.publish(this.article.id).subscribe({ next: a => { this.article = a; this.renderedContent = this.renderMarkdown(a.content); } });
  }

  onArchive(): void {
    if (!this.article) return;
    this.svc.archive(this.article.id).subscribe({ next: a => { this.article = a; this.renderedContent = this.renderMarkdown(a.content); } });
  }

  onDelete(): void {
    if (!this.article || !confirm('Artikel wirklich loeschen? Unterartikel werden eine Ebene nach oben verschoben.')) return;
    this.svc.delete(this.article.id).subscribe({ next: () => this.router.navigate(['/artikel']) });
  }

  rate(stars: number): void {
    if (!this.article) return;
    this.userRating = stars;
    this.svc.submitFeedback(this.article.id, stars).subscribe();
  }

  formatDate(d: string): string {
    return d ? new Date(d).toLocaleDateString('de-DE', { day: '2-digit', month: '2-digit', year: 'numeric' }) : '';
  }

  statusLabel(s: string): string {
    return s === 'PUBLISHED' ? 'Veroeffentlicht' : s === 'DRAFT' ? 'Entwurf' : 'Archiviert';
  }
}
