import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ArtikelService } from '../../services/artikel.service';
import { Artikel } from '../../models/artikel.model';

@Component({
  selector: 'app-startseite',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="welcome card" style="margin-bottom: 1.5rem">
      <h1 style="font-size: 1.375rem; font-weight: 600; color: var(--primary); margin-bottom: 0.375rem">
        Willkommen im Wissensmanagement
      </h1>
      <p style="color: var(--text-muted); font-size: 0.9375rem; line-height: 1.6">
        Hier finden Sie alle wichtigen Informationen, Dokumentationen und Wissensbeitraege
        Ihrer Organisation an einem zentralen Ort.
      </p>
    </div>

    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem; margin-bottom: 2rem">
      <div class="card" style="text-align: center; padding: 1.25rem">
        <div style="font-size: 2rem; font-weight: 700; color: var(--primary)">{{ artikelCount }}</div>
        <div style="font-size: 0.8125rem; color: var(--text-muted)">Wissensbeitraege</div>
      </div>
      <div class="card" style="text-align: center; padding: 1.25rem">
        <div style="font-size: 2rem; font-weight: 700; color: var(--primary)">{{ kategorienCount }}</div>
        <div style="font-size: 0.8125rem; color: var(--text-muted)">Kategorien</div>
      </div>
      <div class="card" style="text-align: center; padding: 1.25rem">
        <a routerLink="/artikel/neu" class="btn btn-primary" style="width: 100%">
          + Neuer Artikel
        </a>
      </div>
    </div>

    <h2 style="font-size: 1.125rem; font-weight: 600; margin-bottom: 1rem">Neueste Beitraege</h2>
    <div style="display: flex; flex-direction: column; gap: 0.75rem">
      <a *ngFor="let a of recentArticles"
         [routerLink]="'/artikel/' + a.id"
         class="card"
         style="text-decoration: none; color: inherit; transition: box-shadow 0.15s"
         onmouseover="this.style.boxShadow='0 2px 8px rgba(0,0,0,0.12)'"
         onmouseout="this.style.boxShadow='0 1px 3px rgba(0,0,0,0.08)'">
        <div style="display: flex; justify-content: space-between; align-items: start; gap: 1rem">
          <div>
            <h3 style="font-size: 0.9375rem; font-weight: 600; margin-bottom: 0.25rem">{{ a.titel }}</h3>
            <p style="font-size: 0.8125rem; color: var(--text-muted); line-height: 1.5">
              {{ a.inhalt | slice:0:120 }}{{ a.inhalt.length > 120 ? '...' : '' }}
            </p>
          </div>
          <span *ngIf="a.kategorie"
                style="padding: 0.25rem 0.75rem; background: #dbeafe; color: #1e40af; font-size: 0.6875rem; font-weight: 500; border-radius: 1rem; white-space: nowrap">
            {{ a.kategorie }}
          </span>
        </div>
      </a>
    </div>

    <div *ngIf="recentArticles.length > 0" style="text-align: center; margin-top: 1.5rem">
      <a routerLink="/artikel" class="btn btn-secondary">Alle Beitraege anzeigen</a>
    </div>
  `,
})
export class StartseiteComponent implements OnInit {
  private artikelService = inject(ArtikelService);

  recentArticles: Artikel[] = [];
  artikelCount = 0;
  kategorienCount = 0;

  ngOnInit(): void {
    this.artikelService.getAll().subscribe({
      next: (articles) => {
        this.recentArticles = articles.slice(0, 5);
      }
    });
    this.artikelService.getStatistik().subscribe({
      next: (stats) => {
        this.artikelCount = stats.gesamt;
        this.kategorienCount = stats.kategorien;
      }
    });
  }
}
