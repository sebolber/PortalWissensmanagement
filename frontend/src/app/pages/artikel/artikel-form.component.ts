import { Component, inject, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ArtikelService } from '../../services/artikel.service';
import { Category, Grouping } from '../../models/artikel.model';

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
          <input type="text" [(ngModel)]="title" name="title" required placeholder="Titel des Artikels"
                 style="resize: vertical;">
        </div>

        <div class="form-group">
          <label>
            Zusammenfassung
            <button type="button" class="btn-generate" (click)="generateSummary()"
                    [disabled]="generatingSummary || !content.trim()" title="Zusammenfassung per KI generieren">
              {{ generatingSummary ? 'Generiert...' : 'KI-Zusammenfassung' }}
            </button>
          </label>
          <textarea [(ngModel)]="summary" name="summary" rows="4" placeholder="Zusammenfassung (optional)"
                    style="resize: vertical;"></textarea>
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
          <div class="form-group">
            <label>Gruppierung</label>
            <select [(ngModel)]="groupingId" name="groupingId">
              <option value="">Keine Gruppierung</option>
              <option *ngFor="let g of groupings" [value]="g.id">{{ g.name }}</option>
            </select>
          </div>
        </div>

        <div class="form-group">
          <label>Tags (kommagetrennt)</label>
          <input type="text" [(ngModel)]="tagsInput" name="tags" placeholder="z.B. Anleitung, Datenschutz">
        </div>

        <div class="form-group">
          <label>Inhalt *</label>
          <div class="toolbar">
            <button type="button" (click)="execCmd('bold')" title="Fett"><b>F</b></button>
            <button type="button" (click)="execCmd('italic')" title="Kursiv"><i>K</i></button>
            <button type="button" (click)="execCmd('underline')" title="Unterstrichen"><u>U</u></button>
            <span class="separator"></span>
            <button type="button" (click)="execCmd('insertUnorderedList')" title="Liste">&#8226;</button>
            <button type="button" (click)="execCmd('insertOrderedList')" title="Nummerierung">1.</button>
          </div>
          <div #contentEditor contenteditable="true" class="rich-editor" (input)="onContentInput()"
               [innerHTML]="contentHtml" style="resize: vertical; overflow: auto;"></div>
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
    .form-group label { display: flex; align-items: center; gap: 0.5rem; font-size: 0.8125rem; font-weight: 500; color: #6b7280; margin-bottom: 0.375rem; }
    .form-group textarea, .form-group input[type="text"] { width: 100%; }
    .form-actions { display: flex; gap: 0.75rem; justify-content: flex-end; }
    .btn-generate { font-size: 0.6875rem; padding: 0.2rem 0.5rem; background: #eff6ff; color: #006EC7; border: 1px solid #bfdbfe; border-radius: 0.375rem; cursor: pointer; white-space: nowrap; }
    .btn-generate:hover:not(:disabled) { background: #dbeafe; }
    .btn-generate:disabled { opacity: 0.5; cursor: not-allowed; }
    .toolbar { display: flex; gap: 0.25rem; padding: 0.375rem; background: #f9fafb; border: 1px solid #e5e7eb; border-bottom: none; border-radius: 0.5rem 0.5rem 0 0; }
    .toolbar button { width: 2rem; height: 2rem; border: 1px solid #e5e7eb; background: #fff; border-radius: 0.25rem; cursor: pointer; font-size: 0.875rem; display: flex; align-items: center; justify-content: center; }
    .toolbar button:hover { background: #eff6ff; border-color: #006EC7; }
    .toolbar .separator { width: 1px; background: #e5e7eb; margin: 0 0.25rem; }
    .rich-editor { min-height: 300px; padding: 0.75rem; border: 1px solid #e5e7eb; border-radius: 0 0 0.5rem 0.5rem; font-size: 0.875rem; line-height: 1.7; background: #fff; outline: none; }
    .rich-editor:focus { border-color: #006EC7; box-shadow: 0 0 0 2px rgba(0,110,199,0.1); }
    @media (max-width: 640px) { .form-row { flex-direction: column; } }
  `]
})
export class ArtikelFormComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private svc = inject(ArtikelService);
  @ViewChild('contentEditor') contentEditorRef!: ElementRef;

  editId: string | null = null;
  title = '';
  content = '';
  contentHtml = '';
  summary = '';
  categoryId = '';
  groupingId = '';
  tagsInput = '';
  changeNote = '';
  categories: Category[] = [];
  groupings: Grouping[] = [];
  saving = false;
  generatingSummary = false;

  // Dictation state
  isDictating = false;
  dictationText = '';
  private recognition: any = null;

  // LLM structuring state
  structuring = false;
  structuredPreview: { title: string; summary: string; content: string } | null = null;

  ngOnInit(): void {
    this.svc.listCategories().subscribe({ next: c => this.categories = c });
    this.svc.listGroupings().subscribe({ next: g => this.groupings = g });
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
          this.contentHtml = a.content;
          this.summary = a.summary || '';
          this.categoryId = a.category?.id || '';
          this.groupingId = a.grouping?.id || '';
          this.tagsInput = a.tags.map(t => t.name).join(', ');
        }
      });
    }
  }

  onContentInput(): void {
    if (this.contentEditorRef) {
      this.content = this.contentEditorRef.nativeElement.innerHTML;
    }
  }

  execCmd(command: string): void {
    document.execCommand(command, false);
    this.contentEditorRef?.nativeElement.focus();
    this.onContentInput();
  }

  generateSummary(): void {
    if (!this.content.trim()) return;
    this.generatingSummary = true;
    const plainText = this.contentEditorRef?.nativeElement?.innerText || this.content;
    this.svc.generateSummary(this.title, plainText).subscribe({
      next: res => {
        this.summary = res.summary;
        this.generatingSummary = false;
      },
      error: () => this.generatingSummary = false,
    });
  }

  onSubmit(): void {
    this.onContentInput();
    if (!this.title.trim() || !this.content.trim()) return;
    this.saving = true;

    const tagNames = this.tagsInput.split(',').map(t => t.trim()).filter(t => t);
    const data: any = {
      title: this.title,
      content: this.content,
      summary: this.summary || null,
      categoryId: this.categoryId || null,
      groupingId: this.groupingId || null,
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
