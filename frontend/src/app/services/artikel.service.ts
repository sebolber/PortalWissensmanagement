import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Artikel } from '../models/artikel.model';

@Injectable({ providedIn: 'root' })
export class ArtikelService {

  private basePath = '/api/artikel';

  constructor(private http: HttpClient) {}

  getAll(kategorie?: string, q?: string): Observable<Artikel[]> {
    const params: Record<string, string> = {};
    if (kategorie) params['kategorie'] = kategorie;
    if (q) params['q'] = q;
    return this.http.get<Artikel[]>(this.basePath, { params });
  }

  getById(id: string): Observable<Artikel> {
    return this.http.get<Artikel>(`${this.basePath}/${id}`);
  }

  getKategorien(): Observable<string[]> {
    return this.http.get<string[]>(`${this.basePath}/kategorien`);
  }

  create(artikel: Partial<Artikel>): Observable<Artikel> {
    return this.http.post<Artikel>(this.basePath, artikel);
  }

  update(id: string, artikel: Partial<Artikel>): Observable<Artikel> {
    return this.http.put<Artikel>(`${this.basePath}/${id}`, artikel);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.basePath}/${id}`);
  }

  getStatistik(): Observable<{ gesamt: number; kategorien: number }> {
    return this.http.get<{ gesamt: number; kategorien: number }>(`${this.basePath}/statistik`);
  }
}
