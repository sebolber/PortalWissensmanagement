import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ArtikelService } from '../../services/artikel.service';
import { PromptConfig } from '../../models/artikel.model';

@Component({
  selector: 'app-konfiguration',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page-header">
      <h1>KI-Prompt-Konfiguration</h1>
      <p class="subtitle">Erstellen Sie Prompts, die bei der Artikelbearbeitung fuer Zusammenfassungen und Inhaltsstrukturierung verwendet werden koennen.</p>
    </div>

    <!-- Prompt-Formular -->
    <div class="card form-card" *ngIf="showForm">
      <h2 class="section-title">{{ editingPrompt ? 'Prompt bearbeiten' : 'Neuer Prompt' }}</h2>
      <form (ngSubmit)="savePrompt()" class="prompt-form">
        <div class="form-row">
          <div class="form-group">
            <label>Name *</label>
            <input type="text" [(ngModel)]="formName" name="name" required placeholder="z.B. Medizinische Zusammenfassung">
          </div>
          <div class="form-group">
            <label>Typ *</label>
            <select [(ngModel)]="formType" name="type">
              <option value="SUMMARY">Zusammenfassung</option>
              <option value="CONTENT">Inhalt</option>
            </select>
          </div>
        </div>
        <div class="form-group">
          <label>Beschreibung</label>
          <input type="text" [(ngModel)]="formDescription" name="description" placeholder="Kurze Beschreibung des Prompts (optional)">
        </div>
        <div class="form-group">
          <label>Prompt-Text *</label>
          <textarea [(ngModel)]="formPromptText" name="promptText" rows="6" required
                    placeholder="Der Prompt wird als System-Anweisung an das LLM gesendet. Der Artikelinhalt wird als Benutzer-Nachricht uebergeben."></textarea>
        </div>
        <div class="form-actions">
          <button type="button" class="btn btn-secondary" (click)="cancelForm()">Abbrechen</button>
          <button type="submit" class="btn btn-primary" [disabled]="!formName.trim() || !formPromptText.trim() || saving">
            {{ saving ? 'Speichert...' : (editingPrompt ? 'Speichern' : 'Erstellen') }}
          </button>
        </div>
      </form>
    </div>

    <!-- Prompt-Listen -->
    <div class="sections">
      <div class="card">
        <div class="section-header">
          <h2 class="section-title">Zusammenfassungs-Prompts</h2>
          <button class="btn btn-primary btn-sm" (click)="openForm('SUMMARY')">+ Neuer Prompt</button>
        </div>
        <p class="hint">Diese Prompts stehen im Artikelformular zur Verfuegung, um eine Zusammenfassung aus dem Inhalt zu generieren.</p>
        <div class="item-list">
          <div *ngFor="let p of summaryPrompts" class="item">
            <div class="item-info">
              <span class="item-name">{{ p.name }}</span>
              <span class="item-desc" *ngIf="p.description">{{ p.description }}</span>
              <span class="item-prompt">{{ p.promptText | slice:0:120 }}{{ p.promptText.length > 120 ? '...' : '' }}</span>
            </div>
            <div class="item-actions">
              <button class="btn-icon" (click)="editPrompt(p)" title="Bearbeiten">&#9998;</button>
              <button class="btn-icon btn-icon-danger" (click)="deletePrompt(p)" title="Loeschen">&times;</button>
            </div>
          </div>
          <div *ngIf="summaryPrompts.length === 0" class="empty">Keine Zusammenfassungs-Prompts konfiguriert.</div>
        </div>
      </div>

      <div class="card">
        <div class="section-header">
          <h2 class="section-title">Inhalts-Prompts</h2>
          <button class="btn btn-primary btn-sm" (click)="openForm('CONTENT')">+ Neuer Prompt</button>
        </div>
        <p class="hint">Diese Prompts stehen im Artikelformular zur Verfuegung, um einkopierten Inhalt neu zu strukturieren oder aufzubereiten.</p>
        <div class="item-list">
          <div *ngFor="let p of contentPrompts" class="item">
            <div class="item-info">
              <span class="item-name">{{ p.name }}</span>
              <span class="item-desc" *ngIf="p.description">{{ p.description }}</span>
              <span class="item-prompt">{{ p.promptText | slice:0:120 }}{{ p.promptText.length > 120 ? '...' : '' }}</span>
            </div>
            <div class="item-actions">
              <button class="btn-icon" (click)="editPrompt(p)" title="Bearbeiten">&#9998;</button>
              <button class="btn-icon btn-icon-danger" (click)="deletePrompt(p)" title="Loeschen">&times;</button>
            </div>
          </div>
          <div *ngIf="contentPrompts.length === 0" class="empty">Keine Inhalts-Prompts konfiguriert.</div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .page-header { margin-bottom: 1.5rem; }
    .page-header h1 { font-size: 1.25rem; font-weight: 600; }
    .subtitle { font-size: 0.8125rem; color: #6b7280; margin-top: 0.25rem; }
    .sections { display: flex; flex-direction: column; gap: 1.5rem; }
    .form-card { margin-bottom: 1.5rem; }
    .section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 0.5rem; }
    .section-title { font-size: 1rem; font-weight: 600; margin-bottom: 0; }
    .hint { font-size: 0.75rem; color: #9ca3af; margin-bottom: 1rem; }
    .prompt-form { display: flex; flex-direction: column; gap: 1rem; }
    .form-row { display: flex; gap: 1rem; }
    .form-row .form-group { flex: 1; }
    .form-group label { display: block; font-size: 0.8125rem; font-weight: 500; color: #6b7280; margin-bottom: 0.375rem; }
    .form-group textarea, .form-group input[type="text"] { width: 100%; }
    .form-group textarea { resize: vertical; font-family: monospace; font-size: 0.8125rem; }
    .form-actions { display: flex; gap: 0.75rem; justify-content: flex-end; }
    .btn-sm { font-size: 0.75rem; padding: 0.375rem 0.75rem; }
    .item-list { display: flex; flex-direction: column; gap: 0.5rem; }
    .item { display: flex; align-items: flex-start; gap: 0.75rem; padding: 0.75rem; background: #f9fafb; border-radius: 0.5rem; }
    .item-info { flex: 1; display: flex; flex-direction: column; gap: 0.25rem; min-width: 0; }
    .item-name { font-size: 0.875rem; font-weight: 600; color: #1f2937; }
    .item-desc { font-size: 0.75rem; color: #6b7280; }
    .item-prompt { font-size: 0.75rem; color: #9ca3af; font-family: monospace; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .item-actions { display: flex; gap: 0.25rem; flex-shrink: 0; }
    .btn-icon { border: none; background: none; font-size: 1.125rem; color: #9ca3af; cursor: pointer; padding: 0.25rem; line-height: 1; border-radius: 0.25rem; }
    .btn-icon:hover { background: #f3f4f6; color: #006EC7; }
    .btn-icon-danger:hover { color: #dc2626; }
    .empty { text-align: center; padding: 1.5rem; color: #9ca3af; font-size: 0.875rem; }
    @media (max-width: 640px) { .form-row { flex-direction: column; } }
  `]
})
export class KonfigurationComponent implements OnInit {
  private svc = inject(ArtikelService);

  summaryPrompts: PromptConfig[] = [];
  contentPrompts: PromptConfig[] = [];

  showForm = false;
  editingPrompt: PromptConfig | null = null;
  formName = '';
  formDescription = '';
  formPromptText = '';
  formType: 'SUMMARY' | 'CONTENT' = 'SUMMARY';
  saving = false;

  ngOnInit(): void {
    this.loadPrompts();
  }

  loadPrompts(): void {
    this.svc.listPrompts('SUMMARY').subscribe({ next: p => this.summaryPrompts = p });
    this.svc.listPrompts('CONTENT').subscribe({ next: p => this.contentPrompts = p });
  }

  openForm(type: 'SUMMARY' | 'CONTENT'): void {
    this.editingPrompt = null;
    this.formName = '';
    this.formDescription = '';
    this.formPromptText = '';
    this.formType = type;
    this.showForm = true;
  }

  editPrompt(p: PromptConfig): void {
    this.editingPrompt = p;
    this.formName = p.name;
    this.formDescription = p.description || '';
    this.formPromptText = p.promptText;
    this.formType = p.promptType;
    this.showForm = true;
  }

  cancelForm(): void {
    this.showForm = false;
    this.editingPrompt = null;
  }

  savePrompt(): void {
    if (!this.formName.trim() || !this.formPromptText.trim()) return;
    this.saving = true;

    const data = {
      name: this.formName.trim(),
      description: this.formDescription.trim() || undefined,
      promptText: this.formPromptText,
      promptType: this.formType as 'SUMMARY' | 'CONTENT'
    };

    const obs = this.editingPrompt
        ? this.svc.updatePrompt(this.editingPrompt.id, data)
        : this.svc.createPrompt(data);

    obs.subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.editingPrompt = null;
        this.loadPrompts();
      },
      error: () => this.saving = false
    });
  }

  deletePrompt(p: PromptConfig): void {
    if (!confirm(`Prompt "${p.name}" wirklich loeschen?`)) return;
    this.svc.deletePrompt(p.id).subscribe({ next: () => this.loadPrompts() });
  }
}
