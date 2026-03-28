import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ArtikelService } from '../../services/artikel.service';
import { ArticleTreeNode, Category, Grouping, PromptConfig } from '../../models/artikel.model';
import { marked } from 'marked';

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
          <label>
            Zusammenfassung
            <span class="ai-actions">
              <select [(ngModel)]="selectedSummaryPromptId" name="summaryPrompt" class="prompt-select"
                      *ngIf="summaryPrompts.length > 0">
                <option value="">Prompt waehlen...</option>
                <optgroup *ngFor="let g of summaryPromptGroups" [label]="g.label">
                  <option *ngFor="let p of g.prompts" [value]="p.id">{{ p.name }}</option>
                </optgroup>
              </select>
              <button type="button" class="btn-generate" (click)="generateSummaryWithPrompt()"
                      [disabled]="generatingSummary || !content.trim() || !selectedSummaryPromptId"
                      *ngIf="summaryPrompts.length > 0"
                      title="Zusammenfassung mit ausgewaehltem Prompt generieren">
                {{ generatingSummary ? 'Generiert...' : 'Generieren' }}
              </button>
              <button type="button" class="btn-generate" (click)="generateSummary()"
                      [disabled]="generatingSummary || !content.trim()" title="Standard-KI-Zusammenfassung">
                {{ generatingSummary ? 'Generiert...' : 'KI-Zusammenfassung' }}
              </button>
            </span>
          </label>
          <textarea [(ngModel)]="summary" name="summary" rows="3" placeholder="Zusammenfassung (optional)"
                    style="resize: vertical;"></textarea>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label>Uebergeordneter Artikel</label>
            <select [(ngModel)]="parentArticleId" name="parentArticleId">
              <option [ngValue]="null">Kein (Root-Artikel)</option>
              <option *ngFor="let node of flatTree" [ngValue]="node.id" [disabled]="node.id === editId">
                {{ getTreePrefix(node.depth) }}{{ node.title }}
              </option>
            </select>
          </div>
          <div class="form-group">
            <label>Kategorie</label>
            <select [(ngModel)]="categoryId" name="categoryId">
              <option [ngValue]="null">Keine Kategorie</option>
              <option *ngFor="let c of categories" [ngValue]="c.id">{{ c.name }}</option>
            </select>
          </div>
          <div class="form-group">
            <label>Gruppierung</label>
            <select [(ngModel)]="groupingId" name="groupingId">
              <option [ngValue]="null">Keine Gruppierung</option>
              <option *ngFor="let g of groupings" [ngValue]="g.id">{{ g.name }}</option>
            </select>
          </div>
        </div>

        <div class="form-group">
          <label>Tags (kommagetrennt)</label>
          <input type="text" [(ngModel)]="tagsInput" name="tags" placeholder="z.B. Anleitung, Datenschutz">
        </div>

        <div class="form-group">
          <label>
            Inhalt * <span class="hint">(Markdown wird unterstuetzt)</span>
            <span class="ai-actions">
              <select [(ngModel)]="selectedContentPromptId" name="contentPrompt" class="prompt-select"
                      *ngIf="contentPrompts.length > 0">
                <option value="">Prompt waehlen...</option>
                <optgroup *ngFor="let g of contentPromptGroups" [label]="g.label">
                  <option *ngFor="let p of g.prompts" [value]="p.id">{{ p.name }}</option>
                </optgroup>
              </select>
              <button type="button" class="btn-generate" (click)="applyContentPrompt()"
                      [disabled]="applyingContent || !content.trim() || !selectedContentPromptId"
                      *ngIf="contentPrompts.length > 0"
                      title="Inhalt mit KI ueberarbeiten">
                {{ applyingContent ? 'Verarbeitet...' : 'KI-Optimierung' }}
              </button>
            </span>
          </label>

          <!-- Editor Tabs -->
          <div class="editor-tabs">
            <button type="button" class="editor-tab" [class.active]="editorMode === 'edit'" (click)="editorMode = 'edit'">Bearbeiten</button>
            <button type="button" class="editor-tab" [class.active]="editorMode === 'preview'" (click)="editorMode = 'preview'">Vorschau</button>
            <button type="button" class="editor-tab" [class.active]="editorMode === 'split'" (click)="editorMode = 'split'">Teilen</button>
            <span class="editor-spacer"></span>
            <!-- Dictation Button -->
            <button type="button" class="dictation-btn" [class.recording]="isDictating"
                    (click)="toggleDictation()" title="Diktierfunktion">
              <span *ngIf="!isDictating">&#127908; Diktieren</span>
              <span *ngIf="isDictating"><span class="recording-dot"></span> Diktat stoppen</span>
            </button>
          </div>

          <!-- Dictation indicator -->
          <div *ngIf="isDictating" class="dictation-area">
            <div class="dictation-indicator">
              <span class="recording-dot"></span>
              Diktat aktiv &ndash; sprechen Sie...
            </div>
            <div class="dictation-text" *ngIf="dictationText">{{ dictationText }}</div>
          </div>

          <!-- Editor content area -->
          <div class="editor-container" [class.split-mode]="editorMode === 'split'">
            <textarea *ngIf="editorMode !== 'preview'" [(ngModel)]="content" name="content"
                      class="markdown-editor" rows="16"
                      placeholder="Markdown-Inhalt eingeben...&#10;&#10;# Ueberschrift&#10;## Unterueberschrift&#10;&#10;- Listenpunkt&#10;- Noch ein Punkt&#10;&#10;**Fett** und *kursiv*"></textarea>
            <div *ngIf="editorMode !== 'edit'" class="markdown-preview" [innerHTML]="renderMarkdown(content)"></div>
          </div>
        </div>

        <!-- KI-Ueberarbeitung Vorschau -->
        <div *ngIf="contentPreview" class="card ki-preview">
          <h3>KI-Ueberarbeitung</h3>
          <div class="ki-compare">
            <div class="ki-original">
              <label>Original:</label>
              <div class="preview-box" [innerHTML]="renderMarkdown(content)"></div>
            </div>
            <div class="ki-suggestion">
              <label>KI-Vorschlag:</label>
              <div class="preview-box" [innerHTML]="renderMarkdown(contentPreview)"></div>
            </div>
          </div>
          <div class="preview-actions">
            <button type="button" class="btn btn-primary btn-sm" (click)="acceptContentPreview()">Uebernehmen</button>
            <button type="button" class="btn btn-secondary btn-sm" (click)="contentPreview = null">Verwerfen</button>
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
    .form-group label { display: flex; align-items: center; gap: 0.5rem; font-size: 0.8125rem; font-weight: 500; color: #6b7280; margin-bottom: 0.375rem; flex-wrap: wrap; }
    .form-group textarea, .form-group input[type="text"] { width: 100%; }
    .form-actions { display: flex; gap: 0.75rem; justify-content: flex-end; }
    .hint { font-size: 0.6875rem; color: #9ca3af; font-weight: 400; }
    .btn-generate { font-size: 0.6875rem; padding: 0.2rem 0.5rem; background: #eff6ff; color: #006EC7; border: 1px solid #bfdbfe; border-radius: 0.375rem; cursor: pointer; white-space: nowrap; }
    .btn-generate:hover:not(:disabled) { background: #dbeafe; }
    .btn-generate:disabled { opacity: 0.5; cursor: not-allowed; }
    .ai-actions { display: inline-flex; align-items: center; gap: 0.375rem; margin-left: auto; }
    .prompt-select { font-size: 0.6875rem; padding: 0.2rem 0.4rem; border: 1px solid #e5e7eb; border-radius: 0.375rem; background: #fff; color: #374151; max-width: 200px; }

    /* Editor tabs */
    .editor-tabs { display: flex; align-items: center; gap: 0; border: 1px solid #e5e7eb; border-bottom: none; border-radius: 0.5rem 0.5rem 0 0; background: #f9fafb; overflow: hidden; }
    .editor-tab { padding: 0.5rem 1rem; border: none; background: none; font-size: 0.8125rem; color: #6b7280; cursor: pointer; border-bottom: 2px solid transparent; }
    .editor-tab:hover { color: #374151; background: #f3f4f6; }
    .editor-tab.active { color: #006EC7; border-bottom-color: #006EC7; background: #fff; font-weight: 500; }
    .editor-spacer { flex: 1; }

    /* Dictation */
    .dictation-btn { padding: 0.375rem 0.75rem; border: 1px solid #e5e7eb; background: #fff; border-radius: 0.375rem; font-size: 0.75rem; cursor: pointer; margin-right: 0.375rem; display: flex; align-items: center; gap: 0.375rem; }
    .dictation-btn:hover { background: #f3f4f6; }
    .dictation-btn.recording { background: #fef2f2; border-color: #fca5a5; color: #dc2626; }
    .recording-dot { display: inline-block; width: 8px; height: 8px; background: #dc2626; border-radius: 50%; animation: pulse 1s infinite; }
    @keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.3; } }
    .dictation-area { padding: 0.75rem; background: #fef2f2; border: 1px solid #fca5a5; border-top: none; font-size: 0.8125rem; }
    .dictation-indicator { display: flex; align-items: center; gap: 0.5rem; color: #dc2626; font-weight: 500; }
    .dictation-text { margin-top: 0.5rem; color: #6b7280; font-style: italic; }

    /* Editor container */
    .editor-container { border: 1px solid #e5e7eb; border-radius: 0 0 0.5rem 0.5rem; overflow: hidden; }
    .editor-container.split-mode { display: grid; grid-template-columns: 1fr 1fr; }
    .markdown-editor { width: 100%; min-height: 400px; padding: 0.75rem; border: none; font-family: 'SF Mono', Monaco, 'Cascadia Code', monospace; font-size: 0.875rem; line-height: 1.7; resize: vertical; outline: none; background: #fff; }
    .markdown-preview { padding: 0.75rem; font-size: 0.9375rem; line-height: 1.8; color: #374151; min-height: 400px; overflow: auto; background: #fefefe; }
    .split-mode .markdown-editor { border-right: 1px solid #e5e7eb; border-radius: 0; }
    .split-mode .markdown-preview { border-left: none; }

    /* KI-Preview */
    .ki-preview { margin-top: 0; border: 2px solid #bfdbfe; background: #f0f9ff; }
    .ki-preview h3 { font-size: 1rem; font-weight: 600; color: #1e40af; margin-bottom: 1rem; }
    .ki-compare { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-bottom: 1rem; }
    .ki-original label, .ki-suggestion label { display: block; font-size: 0.75rem; font-weight: 600; color: #6b7280; text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 0.375rem; }
    .preview-box { padding: 0.75rem; background: #fff; border: 1px solid #e5e7eb; border-radius: 0.375rem; font-size: 0.875rem; line-height: 1.6; max-height: 300px; overflow: auto; }
    .preview-actions { display: flex; gap: 0.75rem; }
    .btn-sm { padding: 0.375rem 0.75rem; font-size: 0.8125rem; }
    @media (max-width: 768px) { .form-row { flex-direction: column; } .ki-compare { grid-template-columns: 1fr; } .editor-container.split-mode { grid-template-columns: 1fr; } }
  `]
})
export class ArtikelFormComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private svc = inject(ArtikelService);

  editId: string | null = null;
  title = '';
  content = '';
  summary = '';
  categoryId: string | null = null;
  groupingId: string | null = null;
  tagsInput = '';
  changeNote = '';
  parentArticleId: string | null = null;
  categories: Category[] = [];
  groupings: Grouping[] = [];
  flatTree: { id: string; title: string; depth: number }[] = [];
  saving = false;
  generatingSummary = false;
  editorMode: 'edit' | 'preview' | 'split' = 'edit';

  // Dictation
  isDictating = false;
  dictationText = '';
  private recognition: any = null;

  // Prompt state
  summaryPrompts: PromptConfig[] = [];
  contentPrompts: PromptConfig[] = [];
  summaryPromptGroups: { label: string; prompts: PromptConfig[] }[] = [];
  contentPromptGroups: { label: string; prompts: PromptConfig[] }[] = [];
  selectedSummaryPromptId = '';
  selectedContentPromptId = '';
  applyingContent = false;
  contentPreview: string | null = null;

  ngOnInit(): void {
    this.svc.listCategories().subscribe({ next: c => this.categories = c });
    this.svc.listGroupings().subscribe({ next: g => this.groupings = g });
    this.svc.getTree().subscribe({ next: tree => this.flatTree = this.flattenTree(tree, 0) });
    this.svc.listPrompts('SUMMARY', true).subscribe({
      next: p => {
        this.summaryPrompts = p;
        this.summaryPromptGroups = this.groupPrompts(p);
      }
    });
    this.svc.listPrompts('CONTENT', true).subscribe({
      next: p => {
        this.contentPrompts = p;
        this.contentPromptGroups = this.groupPrompts(p);
      }
    });

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
          this.categoryId = a.category?.id || null;
          this.groupingId = a.grouping?.id || null;
          this.parentArticleId = a.parentArticleId || null;
          this.tagsInput = a.tags.map(t => t.name).join(', ');
        }
      });
    }
  }

  ngOnDestroy(): void {
    this.stopDictation();
  }

  private groupPrompts(prompts: PromptConfig[]): { label: string; prompts: PromptConfig[] }[] {
    const groups = new Map<string, PromptConfig[]>();
    for (const p of prompts) {
      const key = p.categoryName || 'Allgemein';
      if (!groups.has(key)) groups.set(key, []);
      groups.get(key)!.push(p);
    }
    return Array.from(groups.entries()).map(([label, prompts]) => ({ label, prompts }));
  }

  renderMarkdown(text: string): string {
    if (!text) return '';
    return marked.parse(text, { async: false }) as string;
  }

  generateSummary(): void {
    if (!this.content.trim()) return;
    this.generatingSummary = true;
    this.svc.generateSummary(this.title, this.content).subscribe({
      next: res => { this.summary = res.summary; this.generatingSummary = false; },
      error: (err) => {
        this.generatingSummary = false;
        alert(err.error?.error || 'KI-Zusammenfassung fehlgeschlagen.');
      },
    });
  }

  generateSummaryWithPrompt(): void {
    if (!this.content.trim() || !this.selectedSummaryPromptId) return;
    this.generatingSummary = true;
    this.svc.applyPrompt(this.selectedSummaryPromptId, this.content, this.title).subscribe({
      next: res => { this.summary = res.result; this.generatingSummary = false; },
      error: (err) => {
        this.generatingSummary = false;
        alert(err.error?.error || 'KI-Zusammenfassung fehlgeschlagen.');
      }
    });
  }

  applyContentPrompt(): void {
    if (!this.content.trim() || !this.selectedContentPromptId || this.applyingContent) return;
    this.applyingContent = true;
    this.svc.applyPrompt(this.selectedContentPromptId, this.content, this.title).subscribe({
      next: res => { this.contentPreview = res.result; this.applyingContent = false; },
      error: (err) => {
        this.applyingContent = false;
        alert(err.error?.error || 'Verarbeitung fehlgeschlagen.');
      }
    });
  }

  acceptContentPreview(): void {
    if (!this.contentPreview) return;
    this.content = this.contentPreview;
    this.contentPreview = null;
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
      groupingId: this.groupingId || null,
      tagNames,
      parentArticleId: this.parentArticleId || null,
    };

    if (this.editId) {
      data.changeNote = this.changeNote;
      this.svc.update(this.editId, data).subscribe({
        next: a => this.router.navigate(['/artikel', a.id]),
        error: (err) => { console.error('Failed to update article:', err); this.saving = false; },
      });
    } else {
      this.svc.create(data).subscribe({
        next: a => this.router.navigate(['/artikel', a.id]),
        error: (err) => { console.error('Failed to create article:', err); this.saving = false; },
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
          this.content += (this.content ? '\n' : '') + event.results[i][0].transcript;
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

  // --- Tree helpers ---

  getTreePrefix(depth: number): string {
    return '\u00A0\u00A0\u00A0\u00A0'.repeat(depth);
  }

  private flattenTree(nodes: ArticleTreeNode[], depth: number): { id: string; title: string; depth: number }[] {
    const result: { id: string; title: string; depth: number }[] = [];
    for (const node of nodes) {
      result.push({ id: node.id, title: node.title, depth });
      if (node.children) {
        result.push(...this.flattenTree(node.children, depth + 1));
      }
    }
    return result;
  }
}
