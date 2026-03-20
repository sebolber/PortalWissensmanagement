import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ArtikelService } from '../../services/artikel.service';
import { Category, Tag } from '../../models/artikel.model';

@Component({
  selector: 'app-kategorien',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page-header">
      <h1>Kategorien &amp; Tags verwalten</h1>
    </div>

    <div class="grid">
      <!-- Kategorien -->
      <div class="card">
        <h2 class="section-title">Kategorien</h2>
        <div class="add-row">
          <input type="text" [(ngModel)]="newCategoryName" placeholder="Neue Kategorie..." (keyup.enter)="addCategory()">
          <button class="btn btn-primary" (click)="addCategory()" [disabled]="!newCategoryName.trim()">Hinzufuegen</button>
        </div>
        <div class="item-list">
          <div *ngFor="let c of categories" class="item">
            <span class="item-name">{{ c.name }}</span>
            <span *ngIf="c.description" class="item-desc">{{ c.description }}</span>
            <button class="btn-icon" (click)="deleteCategory(c)" title="Loeschen">&times;</button>
          </div>
          <div *ngIf="categories.length === 0" class="empty">Keine Kategorien vorhanden.</div>
        </div>
      </div>

      <!-- Tags -->
      <div class="card">
        <h2 class="section-title">Tags</h2>
        <div class="add-row">
          <input type="text" [(ngModel)]="newTagName" placeholder="Neuer Tag..." (keyup.enter)="addTag()">
          <button class="btn btn-primary" (click)="addTag()" [disabled]="!newTagName.trim()">Hinzufuegen</button>
        </div>
        <div class="item-list">
          <div *ngFor="let t of tags" class="item">
            <span class="tag-badge">{{ t.name }}</span>
          </div>
          <div *ngIf="tags.length === 0" class="empty">Keine Tags vorhanden.</div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .page-header { margin-bottom: 1.5rem; }
    .page-header h1 { font-size: 1.25rem; font-weight: 600; }
    .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem; }
    @media (max-width: 768px) { .grid { grid-template-columns: 1fr; } }
    .section-title { font-size: 1rem; font-weight: 600; margin-bottom: 1rem; }
    .add-row { display: flex; gap: 0.5rem; margin-bottom: 1rem; }
    .add-row input { flex: 1; }
    .item-list { display: flex; flex-direction: column; gap: 0.5rem; }
    .item { display: flex; align-items: center; gap: 0.75rem; padding: 0.625rem 0.75rem; background: #f9fafb; border-radius: 0.5rem; }
    .item-name { font-size: 0.875rem; font-weight: 500; flex: 1; }
    .item-desc { font-size: 0.75rem; color: #6b7280; }
    .btn-icon { border: none; background: none; font-size: 1.25rem; color: #9ca3af; cursor: pointer; padding: 0 0.25rem; line-height: 1; }
    .btn-icon:hover { color: #dc2626; }
    .tag-badge { padding: 0.2rem 0.6rem; background: #dbeafe; color: #1e40af; font-size: 0.75rem; font-weight: 500; border-radius: 1rem; }
    .empty { text-align: center; padding: 1.5rem; color: #9ca3af; font-size: 0.875rem; }
  `]
})
export class KategorienComponent implements OnInit {
  private svc = inject(ArtikelService);

  categories: Category[] = [];
  tags: Tag[] = [];
  newCategoryName = '';
  newTagName = '';

  ngOnInit(): void {
    this.loadCategories();
    this.loadTags();
  }

  loadCategories(): void {
    this.svc.listCategories().subscribe({ next: c => this.categories = c });
  }

  loadTags(): void {
    this.svc.listTags().subscribe({ next: t => this.tags = t });
  }

  addCategory(): void {
    const name = this.newCategoryName.trim();
    if (!name) return;
    this.svc.createCategory({ name }).subscribe({
      next: () => { this.newCategoryName = ''; this.loadCategories(); }
    });
  }

  addTag(): void {
    const name = this.newTagName.trim();
    if (!name) return;
    this.svc.createTag(name).subscribe({
      next: () => { this.newTagName = ''; this.loadTags(); }
    });
  }

  deleteCategory(c: Category): void {
    if (!confirm(`Kategorie "${c.name}" wirklich loeschen?`)) return;
    this.svc.deleteCategory(c.id).subscribe({
      next: () => this.loadCategories()
    });
  }
}
