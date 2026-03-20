import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ArtikelService } from '../../services/artikel.service';
import { Artikel } from '../../models/artikel.model';

@Component({
  selector: 'app-artikel-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <a routerLink="/artikel" style="display: inline-flex; align-items: center; gap: 0.375rem; font-size: 0.875rem; color: var(--text-muted); margin-bottom: 1rem">
      &larr; Zurueck zur Wissensdatenbank
    </a>

    <div *ngIf="artikel" class="card">
      <div style="display: flex; justify-content: space-between; align-items: start; gap: 1rem; margin-bottom: 1.5rem">
        <div>
          <h1 style="font-size: 1.375rem; font-weight: 600; margin-bottom: 0.375rem">{{ artikel.titel }}</h1>
          <div style="display: flex; gap: 1rem; font-size: 0.8125rem; color: var(--text-muted)">
            <span *ngIf="artikel.autor">Von {{ artikel.autor }}</span>
            <span>Erstellt: {{ formatDate(artikel.erstelltAm) }}</span>
            <span *ngIf="artikel.aktualisiertAm !== artikel.erstelltAm">Aktualisiert: {{ formatDate(artikel.aktualisiertAm) }}</span>
          </div>
        </div>
        <span *ngIf="artikel.kategorie"
              style="padding: 0.25rem 0.75rem; background: #dbeafe; color: #1e40af; font-size: 0.75rem; font-weight: 500; border-radius: 1rem; white-space: nowrap">
          {{ artikel.kategorie }}
        </span>
      </div>

      <div style="font-size: 0.9375rem; line-height: 1.8; color: #374151; white-space: pre-wrap">{{ artikel.inhalt }}</div>

      <div style="display: flex; gap: 0.75rem; margin-top: 2rem; padding-top: 1.5rem; border-top: 1px solid var(--border)">
        <a [routerLink]="'/artikel/' + artikel.id + '/bearbeiten'" class="btn btn-secondary">Bearbeiten</a>
        <button (click)="onDelete()" class="btn btn-danger">Loeschen</button>
      </div>
    </div>

    <div *ngIf="!artikel && !loading" style="text-align: center; padding: 3rem; color: var(--text-muted)">
      Artikel nicht gefunden.
    </div>
  `,
})
export class ArtikelDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private artikelService = inject(ArtikelService);

  artikel: Artikel | null = null;
  loading = true;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.artikelService.getById(id).subscribe({
        next: (a) => { this.artikel = a; this.loading = false; },
        error: () => this.loading = false
      });
    }
  }

  onDelete(): void {
    if (!this.artikel) return;
    this.artikelService.delete(this.artikel.id).subscribe({
      next: () => this.router.navigate(['/artikel'])
    });
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    return new Date(dateStr).toLocaleDateString('de-DE', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }
}
