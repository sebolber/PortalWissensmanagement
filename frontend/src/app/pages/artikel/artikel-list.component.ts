import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ArtikelService } from '../../services/artikel.service';
import { ArticleTreeComponent } from '../../components/article-tree.component';
import { Article, ArticleTreeNode, Category, Grouping } from '../../models/artikel.model';

@Component({
  selector: 'app-artikel-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, ArticleTreeComponent],
  template: `
    <div class="wissensdatenbank-layout">
      <!-- Tree Sidebar -->
      <aside class="tree-sidebar" [class.collapsed]="treeSidebarCollapsed">
        <div class="tree-toggle">
          <button class="btn-icon" (click)="treeSidebarCollapsed = !treeSidebarCollapsed"
                  [title]="treeSidebarCollapsed ? 'Baum einblenden' : 'Baum ausblenden'">
            {{ treeSidebarCollapsed ? '&#9654;' : '&#9664;' }}
          </button>
        </div>
        <app-article-tree *ngIf="!treeSidebarCollapsed"></app-article-tree>
        <div class="filter-field" style="padding: 0.5rem;" *ngIf="!treeSidebarCollapsed">
          <label>Gruppierung</label>
          <select [(ngModel)]="selectedGrouping" (change)="load()">
            <option value="">Alle</option>
            <option *ngFor="let g of groupings" [value]="g.id">{{ g.name }}</option>
          </select>
        </div>
        <div class="filter-field" style="padding: 0 0.5rem 0.5rem;" *ngIf="!treeSidebarCollapsed">
          <label>Status</label>
          <select [(ngModel)]="selectedStatus" (change)="load()">
            <option value="">Alle</option>
            <option value="PUBLISHED">Veroeffentlicht</option>
            <option value="DRAFT">Entwurf</option>
            <option value="ARCHIVED">Archiviert</option>
          </select>
        </div>
      </aside>

      <!-- Main content -->
      <div class="list-main">
        <div class="page-header">
          <div>
            <h1>Wissensdatenbank</h1>
            <div class="view-toggle">
              <button class="toggle-btn" [class.active]="viewMode === 'hierarchy'" (click)="setViewMode('hierarchy')">Hierarchie</button>
              <button class="toggle-btn" [class.active]="viewMode === 'flat'" (click)="setViewMode('flat')">Liste</button>
            </div>
          </div>
          <div class="header-actions">
            <div class="sort-controls">
              <label class="sort-label">Sortierung:</label>
              <select [(ngModel)]="sortOption" (change)="onSortChange()" class="sort-select">
                <option value="createdAt_DESC">Datum (neueste zuerst)</option>
                <option value="createdAt_ASC">Datum (aelteste zuerst)</option>
                <option value="title_ASC">Titel (A-Z)</option>
                <option value="title_DESC">Titel (Z-A)</option>
                <option value="category_ASC">Kategorie</option>
                <option value="content_ASC">Text (A-Z)</option>
              </select>
            </div>
            <button *ngIf="viewMode === 'hierarchy'" class="btn btn-secondary btn-sm"
                    (click)="toggleSubArticles()">
              {{ showSubArticles ? 'Nur Hauptartikel' : 'Alle Artikel anzeigen' }}
            </button>
            <a routerLink="/suche" class="btn btn-secondary">&#128269; Suche</a>
            <a routerLink="/artikel/neu" class="btn btn-primary">+ Neuer Artikel</a>
          </div>
        </div>

        <div class="results-info" *ngIf="totalElements > 0">
          {{ totalElements }} Artikel gefunden
        </div>

        <!-- Hierarchy View -->
        <div *ngIf="viewMode === 'hierarchy'" class="article-list">
          <ng-container *ngFor="let node of filteredHierarchyArticles">
            <a [routerLink]="'/artikel/' + node.id" class="card article-card"
               [style.marginLeft.px]="node.depth * 24"
               [class.sub-article]="node.depth > 0">
              <div class="article-main">
                <div class="article-content">
                  <div class="article-title-row">
                    <span *ngIf="node.depth > 0" class="depth-indicator">&#8627;</span>
                    <h3 [class.sub-title]="node.depth > 0">{{ node.title }}</h3>
                  </div>
                  <p>{{ node.summary || (node.content | slice:0:150) }}{{ !node.summary && (node.content || '').length > 150 ? '...' : '' }}</p>
                </div>
                <div class="article-badges">
                  <span *ngIf="node.category" class="tag">{{ node.category.name }}</span>
                  <span *ngIf="node.grouping" class="grouping-badge">{{ node.grouping.name }}</span>
                  <span class="status-badge" [class]="node.status.toLowerCase()">{{ statusLabel(node.status) }}</span>
                  <span *ngIf="node.childCount > 0" class="children-badge">{{ node.childCount }} Unter</span>
                </div>
              </div>
              <div class="article-meta">
                <span>v{{ node.version }}</span>
                <span>{{ formatDate(node.createdAt) }}</span>
                <span>{{ node.viewCount }} Aufrufe</span>
                <span *ngIf="node.averageRating > 0">{{ node.averageRating.toFixed(1) }} / 5</span>
                <span *ngFor="let t of node.tags" class="meta-tag">{{ t.name }}</span>
              </div>
            </a>
          </ng-container>
        </div>

        <!-- Flat View -->
        <div *ngIf="viewMode === 'flat'" class="article-list">
          <a *ngFor="let a of articles" [routerLink]="'/artikel/' + a.id" class="card article-card">
            <div class="article-main">
              <div class="article-content">
                <h3>{{ a.title }}</h3>
                <div class="article-hierarchy" *ngIf="a.breadcrumb && a.breadcrumb.length > 1">
                  <ng-container *ngFor="let bc of a.breadcrumb; let last = last; let first = first">
                    <span *ngIf="!last && first">{{ bc.title }}</span>
                    <span *ngIf="!last && !first"> &rsaquo; {{ bc.title }}</span>
                  </ng-container>
                </div>
                <p>{{ a.summary || (a.content | slice:0:180) }}{{ !a.summary && a.content.length > 180 ? '...' : '' }}</p>
              </div>
              <div class="article-badges">
                <span *ngIf="a.grouping" class="grouping-badge">{{ a.grouping.name }}</span>
                <span *ngIf="a.category" class="tag">{{ a.category.name }}</span>
                <span class="status-badge" [class]="a.status.toLowerCase()">{{ statusLabel(a.status) }}</span>
                <span *ngIf="a.childCount > 0" class="children-badge">{{ a.childCount }} Unter</span>
              </div>
            </div>
            <div class="article-meta">
              <span>v{{ a.version }}</span>
              <span>{{ formatDate(a.createdAt) }}</span>
              <span>{{ a.viewCount }} Aufrufe</span>
              <span *ngIf="a.averageRating > 0">{{ a.averageRating.toFixed(1) }} / 5</span>
              <span *ngIf="a.depth > 0" class="depth-info">Ebene {{ a.depth + 1 }}</span>
              <span *ngFor="let t of a.tags" class="meta-tag">{{ t.name }}</span>
            </div>
          </a>
        </div>

        <div *ngIf="(viewMode === 'flat' ? articles.length : hierarchyArticles.length) === 0 && !loading" class="empty-state">
          <p>Keine Artikel gefunden.</p>
          <a routerLink="/artikel/neu" class="btn btn-primary" style="margin-top:1rem">Ersten Artikel erstellen</a>
        </div>

        <div *ngIf="viewMode === 'flat' && totalPages > 1" class="pagination">
          <button class="btn btn-secondary btn-sm" [disabled]="currentPage === 0" (click)="goToPage(currentPage - 1)">Zurueck</button>
          <span>Seite {{ currentPage + 1 }} von {{ totalPages }}</span>
          <button class="btn btn-secondary btn-sm" [disabled]="currentPage >= totalPages - 1" (click)="goToPage(currentPage + 1)">Weiter</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .wissensdatenbank-layout { display: flex; gap: 0; min-height: calc(100vh - 7rem); }
    .tree-sidebar { width: 280px; background: #fff; border-right: 1px solid #e5e7eb; flex-shrink: 0; display: flex; flex-direction: column; transition: width 0.2s; }
    .tree-sidebar.collapsed { width: 36px; }
    .tree-toggle { display: flex; justify-content: flex-end; padding: 0.25rem; border-bottom: 1px solid #e5e7eb; }
    .btn-icon { border: none; background: none; cursor: pointer; font-size: 0.75rem; color: #6b7280; padding: 0.25rem 0.375rem; }
    .btn-icon:hover { color: #006EC7; }
    .list-main { flex: 1; min-width: 0; padding: 0 0 0 1rem; }
    .page-header { display: flex; justify-content: space-between; align-items: start; margin-bottom: 1.5rem; flex-wrap: wrap; gap: 1rem; }
    .page-header h1 { font-size: 1.25rem; font-weight: 600; }
    .header-actions { display: flex; gap: 0.5rem; align-items: center; flex-wrap: wrap; }
    .sort-controls { display: flex; align-items: center; gap: 0.375rem; }
    .sort-label { font-size: 0.75rem; color: #6b7280; white-space: nowrap; }
    .sort-select { font-size: 0.8125rem; padding: 0.3rem 0.5rem; border: 1px solid #e5e7eb; border-radius: 0.375rem; background: #fff; color: #374151; cursor: pointer; }
    .sort-select:focus { outline: none; border-color: #006EC7; }
    .view-toggle { display: flex; gap: 0; margin-top: 0.5rem; border: 1px solid #e5e7eb; border-radius: 0.375rem; overflow: hidden; }
    .toggle-btn { padding: 0.25rem 0.75rem; border: none; background: #fff; font-size: 0.75rem; cursor: pointer; color: #6b7280; }
    .toggle-btn:hover { background: #f3f4f6; }
    .toggle-btn.active { background: #006EC7; color: #fff; }
    .filter-field { min-width: 140px; }
    .results-info { font-size: 0.8125rem; color: #6b7280; margin-bottom: 0.75rem; }
    .article-list { display: flex; flex-direction: column; gap: 0.5rem; }
    .article-card { text-decoration: none; color: inherit; transition: box-shadow 0.15s, border-left-color 0.15s; cursor: pointer; }
    .article-card:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.12); }
    .article-card.sub-article { border-left: 3px solid #bfdbfe; background: #fafbfc; }
    .article-card.sub-article:hover { border-left-color: #006EC7; }
    .article-title-row { display: flex; align-items: center; gap: 0.375rem; }
    .depth-indicator { color: #9ca3af; font-size: 1rem; flex-shrink: 0; }
    .article-card h3 { font-size: 0.9375rem; font-weight: 600; margin-bottom: 0.125rem; }
    .article-card h3.sub-title { font-size: 0.875rem; font-weight: 500; }
    .article-hierarchy { font-size: 0.6875rem; color: #9ca3af; margin-bottom: 0.25rem; }
    .article-card p { font-size: 0.8125rem; color: #6b7280; line-height: 1.5; }
    .article-main { display: flex; justify-content: space-between; align-items: start; gap: 1rem; }
    .article-content { flex: 1; min-width: 0; }
    .article-badges { display: flex; flex-direction: column; gap: 0.375rem; align-items: flex-end; flex-shrink: 0; }
    .article-meta { display: flex; gap: 0.75rem; font-size: 0.75rem; color: #9ca3af; margin-top: 0.5rem; align-items: center; flex-wrap: wrap; }
    .meta-tag { padding: 0.1rem 0.4rem; background: #f3f4f6; border-radius: 0.25rem; font-size: 0.6875rem; }
    .tag { padding: 0.2rem 0.6rem; background: #dbeafe; color: #1e40af; font-size: 0.6875rem; font-weight: 500; border-radius: 1rem; white-space: nowrap; }
    .status-badge { padding: 0.15rem 0.5rem; border-radius: 0.25rem; font-size: 0.6875rem; font-weight: 600; text-transform: uppercase; }
    .status-badge.published { background: #dcfce7; color: #166534; }
    .status-badge.draft { background: #fef3c7; color: #92400e; }
    .status-badge.archived { background: #f3f4f6; color: #6b7280; }
    .children-badge { padding: 0.15rem 0.5rem; background: #f0f9ff; color: #0369a1; font-size: 0.6875rem; border-radius: 0.25rem; }
    .grouping-badge { padding: 0.15rem 0.5rem; background: #fef3c7; color: #92400e; font-size: 0.6875rem; font-weight: 500; border-radius: 1rem; white-space: nowrap; }
    .depth-info { padding: 0.1rem 0.4rem; background: #f0f9ff; color: #0369a1; font-size: 0.625rem; border-radius: 0.25rem; }
    .empty-state { text-align: center; padding: 3rem; color: #9ca3af; }
    .pagination { display: flex; align-items: center; justify-content: center; gap: 1rem; margin-top: 1.5rem; font-size: 0.875rem; }
    .btn-sm { padding: 0.375rem 0.75rem; font-size: 0.8125rem; }
    @media (max-width: 768px) { .tree-sidebar { display: none; } .list-main { padding: 0; } }
  `]
})
export class ArtikelListComponent implements OnInit {
  private svc = inject(ArtikelService);

  articles: Article[] = [];
  hierarchyArticles: Article[] = [];
  categories: Category[] = [];
  groupings: Grouping[] = [];
  searchQuery = '';
  selectedCategory = '';
  selectedGrouping = '';
  selectedStatus = '';
  loading = true;
  totalElements = 0;
  totalPages = 0;
  currentPage = 0;
  treeSidebarCollapsed = false;
  viewMode: 'hierarchy' | 'flat' = 'hierarchy';
  showSubArticles = true;
  sortOption = 'createdAt_DESC';

  get filteredHierarchyArticles(): Article[] {
    if (this.showSubArticles) {
      return this.sortArticles(this.hierarchyArticles);
    }
    return this.sortArticles(this.hierarchyArticles.filter(a => a.depth === 0));
  }

  ngOnInit(): void {
    this.load();
    this.loadHierarchy();
    this.svc.listCategories().subscribe({ next: c => this.categories = c });
    this.svc.listGroupings().subscribe({ next: g => this.groupings = g });
  }

  setViewMode(mode: 'hierarchy' | 'flat'): void {
    this.viewMode = mode;
    if (mode === 'hierarchy' && this.hierarchyArticles.length === 0) {
      this.loadHierarchy();
    }
  }

  toggleSubArticles(): void {
    this.showSubArticles = !this.showSubArticles;
  }

  onSortChange(): void {
    if (this.viewMode === 'flat') {
      const [sortBy, sortDir] = this.sortOption.split('_');
      this.currentPage = 0;
      this.load(sortBy, sortDir);
    }
    // Hierarchy-View wird ueber getter filteredHierarchyArticles automatisch sortiert
  }

  loadHierarchy(): void {
    this.svc.getTree().subscribe({
      next: tree => {
        this.hierarchyArticles = [];
        this.flattenTreeToArticles(tree, 0);
      }
    });
  }

  private flattenTreeToArticles(nodes: ArticleTreeNode[], depth: number): void {
    for (const node of nodes) {
      this.svc.getById(node.id, false).subscribe({
        next: article => {
          article.depth = depth;
          const idx = this.findInsertIndex(node.id, nodes, depth);
          this.hierarchyArticles.splice(idx, 0, article);
        }
      });
      if (node.children && node.children.length > 0) {
        this.flattenTreeToArticles(node.children, depth + 1);
      }
    }
  }

  private findInsertIndex(nodeId: string, siblings: ArticleTreeNode[], depth: number): number {
    return this.hierarchyArticles.length;
  }

  private sortArticles(articles: Article[]): Article[] {
    if (!this.showSubArticles) {
      // Nur Hauptartikel: einfach sortieren
      return [...articles].sort((a, b) => this.compareArticles(a, b));
    }
    // Mit Unterartikeln: Hauptartikel sortieren, Unterartikel unter ihrem Parent halten
    const roots = articles.filter(a => a.depth === 0);
    const sortedRoots = [...roots].sort((a, b) => this.compareArticles(a, b));
    const result: Article[] = [];
    for (const root of sortedRoots) {
      result.push(root);
      // Alle Unterartikel dieses Roots (depth > 0 die nach diesem Root kommen)
      const children = articles.filter(a => a.depth > 0 && a.treePath?.startsWith(root.treePath || `/${root.id}/`));
      const sortedChildren = [...children].sort((a, b) => this.compareArticles(a, b));
      result.push(...sortedChildren);
    }
    return result;
  }

  private compareArticles(a: Article, b: Article): number {
    const [field, dir] = this.sortOption.split('_');
    const asc = dir === 'ASC' ? 1 : -1;
    switch (field) {
      case 'title':
        return asc * (a.title || '').localeCompare(b.title || '', 'de');
      case 'createdAt':
        return asc * ((a.createdAt || '').localeCompare(b.createdAt || ''));
      case 'category':
        return asc * ((a.category?.name || 'zzz').localeCompare(b.category?.name || 'zzz', 'de'));
      case 'content':
        return asc * ((a.content || '').localeCompare(b.content || '', 'de'));
      default:
        return 0;
    }
  }

  load(sortBy?: string, sortDir?: string): void {
    this.loading = true;
    const [defaultSort, defaultDir] = this.sortOption.split('_');
    this.svc.list({
      status: this.selectedStatus || undefined,
      q: this.searchQuery || undefined,
      categoryId: this.selectedCategory || undefined,
      groupingId: this.selectedGrouping || undefined,
      page: this.currentPage,
      size: 20,
      sortBy: sortBy || defaultSort,
      sortDir: sortDir || defaultDir,
    }).subscribe({
      next: page => {
        this.articles = page.content;
        this.totalElements = page.totalElements;
        this.totalPages = page.totalPages;
        this.loading = false;
      },
      error: (err) => { console.error('Failed to load articles:', err); this.loading = false; },
    });
  }

  onSearch(): void { this.currentPage = 0; this.load(); }
  goToPage(p: number): void { this.currentPage = p; this.load(); }

  formatDate(d: string): string {
    return d ? new Date(d).toLocaleDateString('de-DE', { day: '2-digit', month: '2-digit', year: 'numeric' }) : '';
  }

  statusLabel(s: string): string {
    return s === 'PUBLISHED' ? 'Veroeffentlicht' : s === 'DRAFT' ? 'Entwurf' : 'Archiviert';
  }
}
