import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ArtikelService } from '../../services/artikel.service';

@Component({
  selector: 'app-artikel-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <a [routerLink]="editId ? '/artikel/' + editId : '/artikel'"
       style="display: inline-flex; align-items: center; gap: 0.375rem; font-size: 0.875rem; color: var(--text-muted); margin-bottom: 1rem">
      &larr; Zurueck
    </a>

    <div class="card">
      <h1 style="font-size: 1.25rem; font-weight: 600; margin-bottom: 1.5rem">
        {{ editId ? 'Artikel bearbeiten' : 'Neuer Artikel' }}
      </h1>

      <form (ngSubmit)="onSubmit()" style="display: flex; flex-direction: column; gap: 1.25rem">
        <div>
          <label>Titel *</label>
          <input type="text" [(ngModel)]="titel" name="titel" required placeholder="Titel des Artikels">
        </div>

        <div style="display: flex; gap: 1rem">
          <div style="flex: 1">
            <label>Kategorie</label>
            <input type="text" [(ngModel)]="kategorie" name="kategorie" placeholder="z.B. Anleitungen, Compliance">
          </div>
          <div style="flex: 1">
            <label>Autor</label>
            <input type="text" [(ngModel)]="autor" name="autor" placeholder="Name des Autors">
          </div>
        </div>

        <div>
          <label>Inhalt *</label>
          <textarea [(ngModel)]="inhalt" name="inhalt" required rows="12" placeholder="Artikelinhalt..."></textarea>
        </div>

        <div style="display: flex; gap: 0.75rem; justify-content: flex-end">
          <a [routerLink]="editId ? '/artikel/' + editId : '/artikel'" class="btn btn-secondary">Abbrechen</a>
          <button type="submit" class="btn btn-primary" [disabled]="saving">
            {{ saving ? 'Speichert...' : (editId ? 'Speichern' : 'Erstellen') }}
          </button>
        </div>
      </form>
    </div>
  `,
})
export class ArtikelFormComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private artikelService = inject(ArtikelService);

  editId: string | null = null;
  titel = '';
  inhalt = '';
  kategorie = '';
  autor = '';
  saving = false;

  ngOnInit(): void {
    this.editId = this.route.snapshot.paramMap.get('id');
    if (this.editId) {
      this.artikelService.getById(this.editId).subscribe({
        next: (a) => {
          this.titel = a.titel;
          this.inhalt = a.inhalt;
          this.kategorie = a.kategorie || '';
          this.autor = a.autor || '';
        }
      });
    }
  }

  onSubmit(): void {
    if (!this.titel.trim() || !this.inhalt.trim()) return;
    this.saving = true;
    const data = { titel: this.titel, inhalt: this.inhalt, kategorie: this.kategorie, autor: this.autor };

    const request = this.editId
      ? this.artikelService.update(this.editId, data)
      : this.artikelService.create(data);

    request.subscribe({
      next: (a) => this.router.navigate(['/artikel', a.id]),
      error: () => this.saving = false
    });
  }
}
