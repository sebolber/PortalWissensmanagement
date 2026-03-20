import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ArtikelService } from '../../services/artikel.service';
import { Category, Tag } from '../../models/artikel.model';

@Component({
  selector: 'app-artikel-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <a [routerLink]="editId ? '/artikel/' + editId : '/artikel'" class="back-link">&larr; Zurueck</a>

    <div class="card">
      <h1 class="form-title">{{ editId ? 'Artikel bearbeiten' : 'Neuer Artikel' }}</h1>

      <form (ngSubmit)="onSubmit()" class="form-grid">
        <div class="form-group">
          <label>Titel *</label>
          <input type="text" [(ngModel)]="title" name="title" required placeholder="Titel des Artikels">
        </div>

        <div class="form-group">
          <label>Zusammenfassung</label>
          <input type="text" [(ngModel)]="summary" name="summary" placeholder="Kurze Zusammenfassung (optional)">
        </div>

        <div class="form-row">
          <div class="form-group">
            <label>Kategorie</label>
            <select [(ngModel)]="categoryId" name="categoryId">
              <option value="">Keine Kategorie</option>
              <option *ngFor="let c of categories" [value]="c.id">{{ c.name }}</option>
            </select>
          </div>
          <div class="form-group">
            <label>Tags (kommagetrennt)</label>
            <input type="text" [(ngModel)]="tagsInput" name="tags" placeholder="z.B. Anleitung, Datenschutz">
          </div>
        </div>

        <div class="form-group">
          <label>Inhalt *</label>
          <textarea [(ngModel)]="content" name="content" required rows="16" placeholder="Artikelinhalt..."></textarea>
        </div>

        <div *ngIf="editId" class="form-group">
          <label>Aenderungsnotiz</label>
          <input type="text" [(ngModel)]="changeNote" name="changeNote" placeholder="Was wurde geaendert?">
        </div>

        <div class="form-actions">
          <a [routerLink]="editId ? '/artikel/' + editId : '/artikel'" class="btn btn-secondary">Abbrechen</a>
          <button type="submit" class="btn btn-primary" [disabled]="saving">
            {{ saving ? 'Speichert...' : (editId ? 'Speichern' : 'Erstellen') }}
          </button>
        </div>
      </form>
    </div>
  `,
  styles: [`
    .back-link { display: inline-flex; align-items: center; gap: 0.375rem; font-size: 0.875rem; color: #6b7280; margin-bottom: 1rem; text-decoration: none; }
    .back-link:hover { color: #006EC7; }
    .form-title { font-size: 1.25rem; font-weight: 600; margin-bottom: 1.5rem; }
    .form-grid { display: flex; flex-direction: column; gap: 1.25rem; }
    .form-row { display: flex; gap: 1rem; }
    .form-row .form-group { flex: 1; }
    .form-group label { display: block; font-size: 0.8125rem; font-weight: 500; color: #6b7280; margin-bottom: 0.375rem; }
    .form-actions { display: flex; gap: 0.75rem; justify-content: flex-end; }
    @media (max-width: 640px) { .form-row { flex-direction: column; } }
  `]
})
export class ArtikelFormComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private svc = inject(ArtikelService);

  editId: string | null = null;
  title = '';
  content = '';
  summary = '';
  categoryId = '';
  tagsInput = '';
  changeNote = '';
  categories: Category[] = [];
  saving = false;

  ngOnInit(): void {
    this.svc.listCategories().subscribe({ next: c => this.categories = c });
    this.editId = this.route.snapshot.paramMap.get('id');
    if (this.editId) {
      this.svc.getById(this.editId, false).subscribe({
        next: a => {
          this.title = a.title;
          this.content = a.content;
          this.summary = a.summary || '';
          this.categoryId = a.category?.id || '';
          this.tagsInput = a.tags.map(t => t.name).join(', ');
        }
      });
    }
  }

  onSubmit(): void {
    if (!this.title.trim() || !this.content.trim()) return;
    this.saving = true;

    const tagNames = this.tagsInput.split(',').map(t => t.trim()).filter(t => t);
    const data: any = {
      title: this.title,
      content: this.content,
      summary: this.summary || null,
      categoryId: this.categoryId || null,
      tagNames,
    };

    if (this.editId) {
      data.changeNote = this.changeNote;
      this.svc.update(this.editId, data).subscribe({
        next: a => this.router.navigate(['/artikel', a.id]),
        error: () => this.saving = false,
      });
    } else {
      this.svc.create(data).subscribe({
        next: a => this.router.navigate(['/artikel', a.id]),
        error: () => this.saving = false,
      });
    }
  }
}
