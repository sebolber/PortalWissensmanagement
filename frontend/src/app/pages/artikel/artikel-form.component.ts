import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ArtikelService } from '../../services/artikel.service';
import { Category, ArticleTreeNode } from '../../models/artikel.model';

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
            <label>Uebergeordneter Artikel</label>
            <select [(ngModel)]="parentArticleId" name="parentArticleId">
              <option value="">Kein (Root-Artikel)</option>
              <option *ngFor="let node of flatTree" [value]="node.id" [disabled]="node.id === editId">
                {{ getTreePrefix(node.depth) }}{{ node.title }}
              </option>
            </select>
          </div>
          <div class="form-group">
            <label>Kategorie</label>
            <select [(ngModel)]="categoryId" name="categoryId">
              <option value="">Keine Kategorie</option>
              <option *ngFor="let c of categories" [value]="c.id">{{ c.name }}</option>
            </select>
          </div>
        </div>

        <div class="form-group">
          <label>Tags (kommagetrennt)</label>
          <input type="text" [(ngModel)]="tagsInput" name="tags" placeholder="z.B. Anleitung, Datenschutz">
        </div>

        <div class="form-group">
          <label>Inhalt *</label>
          <div class="content-toolbar">
            <button type="button" class="btn btn-secondary btn-sm" (click)="toggleDictation()"
                    [class.active]="isDictating">
              {{ isDictating ? '&#9632; Diktat stoppen' : '&#127908; Diktat starten' }}
            </button>
            <button type="button" class="btn btn-secondary btn-sm" (click)="structureContent()"
                    [disabled]="!content.trim() || structuring">
              {{ structuring ? 'Strukturiert...' : '&#10024; Mit KI strukturieren' }}
            </button>
          </div>
          <textarea [(ngModel)]="content" name="content" required rows="16" placeholder="Artikelinhalt..."></textarea>
        </div>

        <!-- Dictation area -->
        <div *ngIf="isDictating" class="dictation-area card">
          <div class="dictation-indicator">
            <span class="recording-dot"></span>
            Diktat aktiv &ndash; sprechen Sie...
          </div>
          <div class="dictation-text" *ngIf="dictationText">{{ dictationText }}</div>
        </div>

        <!-- LLM structuring preview -->
        <div *ngIf="structuredPreview" class="card structure-preview">
          <h3>KI-Vorschlag</h3>
          <div class="preview-section" *ngIf="structuredPreview.title">
            <label>Titel:</label>
            <p>{{ structuredPreview.title }}</p>
          </div>
          <div class="preview-section" *ngIf="structuredPreview.summary">
            <label>Zusammenfassung:</label>
            <p>{{ structuredPreview.summary }}</p>
          </div>
          <div class="preview-section">
            <label>Inhalt:</label>
            <div class="preview-content">{{ structuredPreview.content }}</div>
          </div>
          <div class="preview-actions">
            <button type="button" class="btn btn-primary btn-sm" (click)="acceptStructured()">Uebernehmen</button>
            <button type="button" class="btn btn-secondary btn-sm" (click)="structuredPreview = null">Verwerfen</button>
          </div>
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
    .content-toolbar { display: flex; gap: 0.5rem; margin-bottom: 0.5rem; }
    .btn-sm { padding: 0.375rem 0.75rem; font-size: 0.8125rem; }
    .btn-sm.active { background: #dc2626; color: #fff; border-color: #dc2626; }
    .dictation-area { background: #fef3c7; border: 1px solid #fbbf24; padding: 1rem; }
    .dictation-indicator { display: flex; align-items: center; gap: 0.5rem; font-size: 0.875rem; font-weight: 500; color: #92400e; }
    .recording-dot { width: 10px; height: 10px; border-radius: 50%; background: #dc2626; animation: pulse 1.5s infinite; }
    @keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.3; } }
    .dictation-text { margin-top: 0.5rem; font-size: 0.875rem; color: #374151; white-space: pre-wrap; }
    .structure-preview { margin-top: 0.5rem; border: 1px solid #dbeafe; background: #f0f9ff; }
    .structure-preview h3 { font-size: 0.9375rem; font-weight: 600; color: #006EC7; margin-bottom: 0.75rem; }
    .preview-section { margin-bottom: 0.75rem; }
    .preview-section label { font-size: 0.75rem; font-weight: 600; color: #6b7280; text-transform: uppercase; display: block; margin-bottom: 0.25rem; }
    .preview-section p { font-size: 0.875rem; color: #374151; }
    .preview-content { font-size: 0.8125rem; color: #374151; white-space: pre-wrap; max-height: 200px; overflow-y: auto; background: #fff; padding: 0.5rem; border-radius: 0.375rem; }
    .preview-actions { display: flex; gap: 0.5rem; margin-top: 0.75rem; }
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
  parentArticleId = '';
  tagsInput = '';
  changeNote = '';
  categories: Category[] = [];
  flatTree: { id: string; title: string; depth: number }[] = [];
  saving = false;

  // Dictation state
  isDictating = false;
  dictationText = '';
  private recognition: any = null;

  // LLM structuring state
  structuring = false;
  structuredPreview: { title: string; summary: string; content: string } | null = null;

  ngOnInit(): void {
    this.svc.listCategories().subscribe({ next: c => this.categories = c });
    this.loadTree();

    this.editId = this.route.snapshot.paramMap.get('id');
    const parentParam = this.route.snapshot.queryParamMap.get('parent');
    if (parentParam) {
      this.parentArticleId = parentParam;
    }

    if (this.editId) {
      this.svc.getById(this.editId, false).subscribe({
        next: a => {
          this.title = a.title;
          this.content = a.content;
          this.summary = a.summary || '';
          this.categoryId = a.category?.id || '';
          this.parentArticleId = a.parentArticleId || '';
          this.tagsInput = a.tags.map(t => t.name).join(', ');
        }
      });
    }
  }

  private loadTree(): void {
    this.svc.getTree().subscribe({
      next: tree => {
        this.flatTree = [];
        this.flattenTree(tree, 0);
      }
    });
  }

  private flattenTree(nodes: ArticleTreeNode[], depth: number): void {
    for (const node of nodes) {
      this.flatTree.push({ id: node.id, title: node.title, depth });
      if (node.children) {
        this.flattenTree(node.children, depth + 1);
      }
    }
  }

  getTreePrefix(depth: number): string {
    return '\u00A0\u00A0'.repeat(depth) + (depth > 0 ? '└ ' : '');
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
      parentArticleId: this.parentArticleId || null,
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

  // --- Dictation ---

  toggleDictation(): void {
    if (this.isDictating) {
      this.stopDictation();
    } else {
      this.startDictation();
    }
  }

  private startDictation(): void {
    const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SpeechRecognition) {
      alert('Spracherkennung wird von diesem Browser nicht unterstuetzt. Bitte verwenden Sie Chrome oder Edge.');
      return;
    }

    this.recognition = new SpeechRecognition();
    this.recognition.lang = 'de-DE';
    this.recognition.continuous = true;
    this.recognition.interimResults = true;

    this.recognition.onresult = (event: any) => {
      let transcript = '';
      for (let i = event.resultIndex; i < event.results.length; i++) {
        transcript += event.results[i][0].transcript;
        if (event.results[i].isFinal) {
          this.content += (this.content ? ' ' : '') + event.results[i][0].transcript;
          this.dictationText = '';
        } else {
          this.dictationText = transcript;
        }
      }
    };

    this.recognition.onerror = () => {
      this.isDictating = false;
    };

    this.recognition.onend = () => {
      if (this.isDictating) {
        // Restart if still dictating (browser auto-stops)
        this.recognition.start();
      }
    };

    this.recognition.start();
    this.isDictating = true;
  }

  private stopDictation(): void {
    this.isDictating = false;
    if (this.recognition) {
      this.recognition.stop();
      this.recognition = null;
    }
    this.dictationText = '';
  }

  // --- LLM Structuring ---

  structureContent(): void {
    if (!this.content.trim() || this.structuring) return;
    this.structuring = true;

    this.svc.structureText(this.content).subscribe({
      next: result => {
        this.structuredPreview = result;
        this.structuring = false;
      },
      error: () => {
        this.structuring = false;
        alert('Strukturierung fehlgeschlagen. Bitte versuchen Sie es erneut.');
      }
    });
  }

  acceptStructured(): void {
    if (!this.structuredPreview) return;
    if (this.structuredPreview.title && !this.title) {
      this.title = this.structuredPreview.title;
    }
    if (this.structuredPreview.summary) {
      this.summary = this.structuredPreview.summary;
    }
    this.content = this.structuredPreview.content;
    this.structuredPreview = null;
  }
}
