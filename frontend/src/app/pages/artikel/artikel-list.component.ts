import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ArtikelService } from '../../services/artikel.service';
import { Artikel } from '../../models/artikel.model';

@Component({
  selector: 'app-artikel-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; flex-wrap: wrap; gap: 1rem">
      <h1 style="font-size: 1.25rem; font-weight: 600">Wissensdatenbank</h1>
      <a routerLink="/artikel/neu" class="btn btn-primary">+ Neuer Artikel</a>
    </div>

    <!-- Search & Filter -->
    <div class="card" style="margin-bottom: 1.5rem; display: flex; gap: 0.75rem; flex-wrap: wrap; align-items: end">
      <div style="flex: 1; min-width: 200px">
        <label>Suche</label>
        <input type="text" [(ngModel)]="searchQuery" (input)="onSearch()" placeholder="Artikel durchsuchen...">
      </div>
      <div style="min-width: 160px">
        <label>Kategorie</label>
        <select [(ngModel)]="selectedKategorie" (change)="onFilter()">
          <option value="">Alle Kategorien</option>
          <option *ngFor="let k of kategorien" [value]="k">{{ k }}</option>
        </select>
      </div>
    </div>

    <!-- Results -->
    <div style="display: flex; flex-direction: column; gap: 0.75rem">
      <a *ngFor="let a of articles"
         [routerLink]="'/artikel/' + a.id"
         class="card"
         style="text-decoration: none; color: inherit; transition: box-shadow 0.15s"
         onmouseover="this.style.boxShadow='0 2px 8px rgba(0,0,0,0.12)'"
         onmouseout="this.style.boxShadow='0 1px 3px rgba(0,0,0,0.08)'">
        <div style="display: flex; justify-content: space-between; align-items: start; gap: 1rem">
          <div style="flex: 1">
            <h3 style="font-size: 0.9375rem; font-weight: 600; margin-bottom: 0.25rem">{{ a.titel }}</h3>
            <p style="font-size: 0.8125rem; color: var(--text-muted); line-height: 1.5; margin-bottom: 0.5rem">
              {{ a.inhalt | slice:0:160 }}{{ a.inhalt.length > 160 ? '...' : '' }}
            </p>
            <div style="display: flex; gap: 1rem; font-size: 0.75rem; color: #9ca3af">
              <span *ngIf="a.autor">Von {{ a.autor }}</span>
              <span>{{ formatDate(a.erstelltAm) }}</span>
            </div>
          </div>
          <span *ngIf="a.kategorie"
                style="padding: 0.25rem 0.75rem; background: #dbeafe; color: #1e40af; font-size: 0.6875rem; font-weight: 500; border-radius: 1rem; white-space: nowrap; flex-shrink: 0">
            {{ a.kategorie }}
          </span>
        </div>
      </a>
    </div>

    <div *ngIf="articles.length === 0 && !loading" style="text-align: center; padding: 3rem; color: var(--text-muted)">
      <p style="font-size: 0.9375rem">Keine Artikel gefunden.</p>
      <a routerLink="/artikel/neu" class="btn btn-primary" style="margin-top: 1rem">Ersten Artikel erstellen</a>
    </div>
  `,
})
export class ArtikelListComponent implements OnInit {
  private artikelService = inject(ArtikelService);

  articles: Artikel[] = [];
  kategorien: string[] = [];
  searchQuery = '';
  selectedKategorie = '';
  loading = true;

  ngOnInit(): void {
    this.load();
    this.artikelService.getKategorien().subscribe({
      next: (k) => this.kategorien = k
    });
  }

  load(): void {
    this.loading = true;
    this.artikelService.getAll(this.selectedKategorie || undefined, this.searchQuery || undefined).subscribe({
      next: (articles) => {
        this.articles = articles;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  onSearch(): void { this.load(); }
  onFilter(): void { this.searchQuery = ''; this.load(); }

  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    return new Date(dateStr).toLocaleDateString('de-DE', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }
}
