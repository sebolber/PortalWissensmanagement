import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ArtikelService } from '../../services/artikel.service';
import { Article, ArticleVersion } from '../../models/artikel.model';

@Component({
  selector: 'app-artikel-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <a routerLink="/artikel" class="back-link">&larr; Zurueck zur Wissensdatenbank</a>

    <div *ngIf="article" class="card">
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
        </div>
      </div>

      <div *ngIf="article.tags.length > 0" class="tag-row">
        <span *ngFor="let t of article.tags" class="meta-tag">{{ t.name }}</span>
      </div>

      <div *ngIf="article.summary" class="summary-box">
        <strong>Zusammenfassung:</strong> {{ article.summary }}
      </div>

      <div class="content-area">{{ article.content }}</div>

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
        </div>
        <button (click)="onDelete()" class="btn btn-danger">Loeschen</button>
      </div>
    </div>

    <!-- Versionshistorie -->
    <div *ngIf="versions.length > 0" class="card" style="margin-top:1rem">
      <h2 style="font-size:1rem;font-weight:600;margin-bottom:1rem">Versionshistorie</h2>
      <div *ngFor="let v of versions" class="version-item">
        <div class="version-header">
          <strong>Version {{ v.version }}</strong>
          <span>{{ formatDate(v.changedAt) }}</span>
        </div>
        <div *ngIf="v.changeNote" class="version-note">{{ v.changeNote }}</div>
      </div>
    </div>

    <div *ngIf="!article && !loading" class="empty-state">Artikel nicht gefunden.</div>
  `,
  styles: [`
    .back-link { display: inline-flex; align-items: center; gap: 0.375rem; font-size: 0.875rem; color: #6b7280; margin-bottom: 1rem; text-decoration: none; }
    .back-link:hover { color: #006EC7; }
    .detail-header { display: flex; justify-content: space-between; align-items: start; gap: 1rem; margin-bottom: 1rem; }
    .detail-header h1 { font-size: 1.375rem; font-weight: 600; margin-bottom: 0.375rem; }
    .meta-row { display: flex; gap: 0.75rem; font-size: 0.8125rem; color: #6b7280; align-items: center; flex-wrap: wrap; }
    .tag { padding: 0.2rem 0.6rem; background: #dbeafe; color: #1e40af; font-size: 0.75rem; font-weight: 500; border-radius: 1rem; }
    .tag-row { display: flex; gap: 0.375rem; flex-wrap: wrap; margin-bottom: 1rem; }
    .meta-tag { padding: 0.15rem 0.5rem; background: #f3f4f6; border-radius: 0.25rem; font-size: 0.75rem; color: #6b7280; }
    .summary-box { padding: 0.75rem 1rem; background: #f0f9ff; border-radius: 0.5rem; font-size: 0.875rem; color: #1e40af; margin-bottom: 1.5rem; line-height: 1.6; }
    .content-area { font-size: 0.9375rem; line-height: 1.8; color: #374151; white-space: pre-wrap; margin-bottom: 1.5rem; }
    .status-badge { padding: 0.15rem 0.5rem; border-radius: 0.25rem; font-size: 0.6875rem; font-weight: 600; text-transform: uppercase; }
    .status-badge.published { background: #dcfce7; color: #166534; }
    .status-badge.draft { background: #fef3c7; color: #92400e; }
    .status-badge.archived { background: #f3f4f6; color: #6b7280; }
    .rating-section { display: flex; align-items: center; gap: 0.375rem; margin-bottom: 1.5rem; padding-top: 1rem; border-top: 1px solid #e5e7eb; }
    .rating-label { font-size: 0.875rem; color: #6b7280; margin-right: 0.25rem; }
    .star-btn { border: none; background: none; font-size: 1.25rem; color: #d1d5db; cursor: pointer; padding: 0; }
    .star-btn.active { color: #f59e0b; }
    .star-btn:hover { color: #f59e0b; }
    .rating-info { font-size: 0.75rem; color: #9ca3af; margin-left: 0.5rem; }
    .action-bar { display: flex; justify-content: space-between; padding-top: 1.5rem; border-top: 1px solid #e5e7eb; }
    .action-group { display: flex; gap: 0.75rem; }
    .version-item { padding: 0.75rem 0; border-bottom: 1px solid #f3f4f6; }
    .version-item:last-child { border-bottom: none; }
    .version-header { display: flex; justify-content: space-between; font-size: 0.875rem; }
    .version-note { font-size: 0.8125rem; color: #6b7280; margin-top: 0.25rem; }
    .empty-state { text-align: center; padding: 3rem; color: #9ca3af; }
  `]
})
export class ArtikelDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private svc = inject(ArtikelService);

  article: Article | null = null;
  versions: ArticleVersion[] = [];
  loading = true;
  userRating = 0;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.svc.getById(id).subscribe({
        next: a => { this.article = a; this.loading = false; },
        error: () => this.loading = false,
      });
      this.svc.getVersions(id).subscribe({ next: v => this.versions = v });
    }
  }

  onPublish(): void {
    if (!this.article) return;
    this.svc.publish(this.article.id).subscribe({ next: a => this.article = a });
  }

  onArchive(): void {
    if (!this.article) return;
    this.svc.archive(this.article.id).subscribe({ next: a => this.article = a });
  }

  onDelete(): void {
    if (!this.article || !confirm('Artikel wirklich loeschen?')) return;
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
