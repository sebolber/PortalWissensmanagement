import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ArtikelService } from '../../services/artikel.service';
import { PromptConfig, PromptCategory } from '../../models/artikel.model';

@Component({
  selector: 'app-konfiguration',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page-header">
      <h1>Konfiguration</h1>
      <p class="subtitle">Verwaltung von KI-Prompts, Prompt-Kategorien und Daten-Export/Import.</p>
    </div>

    <!-- Tab Navigation -->
    <div class="tab-bar">
      <button class="tab" [class.active]="activeTab === 'prompts'" (click)="activeTab = 'prompts'">KI-Prompts</button>
      <button class="tab" [class.active]="activeTab === 'categories'" (click)="activeTab = 'categories'">Prompt-Kategorien</button>
      <button class="tab" [class.active]="activeTab === 'exportimport'" (click)="activeTab = 'exportimport'">Export / Import</button>
    </div>

    <!-- ===================== PROMPTS TAB ===================== -->
    <div *ngIf="activeTab === 'prompts'">

      <!-- Prompt Form -->
      <div class="card form-card" *ngIf="showPromptForm">
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
          <div class="form-row">
            <div class="form-group">
              <label>Kategorie</label>
              <select [(ngModel)]="formCategoryId" name="categoryId">
                <option value="">Keine Kategorie</option>
                <option *ngFor="let c of promptCategories" [value]="c.id">{{ c.name }}</option>
              </select>
            </div>
            <div class="form-group form-group-small">
              <label>Sortierung</label>
              <input type="number" [(ngModel)]="formSortOrder" name="sortOrder" min="0">
            </div>
            <div class="form-group form-group-small">
              <label>Aktiv</label>
              <label class="toggle-label">
                <input type="checkbox" [(ngModel)]="formActive" name="active">
                <span>{{ formActive ? 'Ja' : 'Nein' }}</span>
              </label>
            </div>
          </div>
          <div class="form-group">
            <label>Beschreibung</label>
            <input type="text" [(ngModel)]="formDescription" name="description" placeholder="Kurze Beschreibung des Prompts (optional)">
          </div>
          <div class="form-group">
            <label>Prompt-Text *</label>
            <textarea [(ngModel)]="formPromptText" name="promptText" rows="6" required
                      placeholder="Der Prompt wird als System-Anweisung an das LLM gesendet."></textarea>
          </div>
          <div class="form-actions">
            <button type="button" class="btn btn-secondary" (click)="cancelPromptForm()">Abbrechen</button>
            <button type="submit" class="btn btn-primary" [disabled]="!formName.trim() || !formPromptText.trim() || saving">
              {{ saving ? 'Speichert...' : (editingPrompt ? 'Speichern' : 'Erstellen') }}
            </button>
          </div>
        </form>
      </div>

      <!-- Prompt List -->
      <div class="card">
        <div class="section-header">
          <h2 class="section-title">Alle Prompts</h2>
          <button class="btn btn-primary btn-sm" (click)="openPromptForm()">+ Neuer Prompt</button>
        </div>

        <!-- Filter -->
        <div class="filter-row">
          <select [(ngModel)]="promptFilter" (change)="filterPrompts()" class="filter-select">
            <option value="">Alle Typen</option>
            <option value="SUMMARY">Zusammenfassung</option>
            <option value="CONTENT">Inhalt</option>
          </select>
          <select [(ngModel)]="promptCategoryFilter" (change)="filterPrompts()" class="filter-select">
            <option value="">Alle Kategorien</option>
            <option *ngFor="let c of promptCategories" [value]="c.id">{{ c.name }}</option>
          </select>
        </div>

        <div class="item-list">
          <div *ngFor="let p of filteredPrompts" class="item" [class.inactive]="!p.active">
            <div class="item-info">
              <div class="item-header">
                <span class="item-name">{{ p.name }}</span>
                <span class="type-badge" [class]="p.promptType.toLowerCase()">{{ p.promptType === 'SUMMARY' ? 'Zusammenfassung' : 'Inhalt' }}</span>
                <span class="category-badge" *ngIf="p.categoryName">{{ p.categoryName }}</span>
                <span class="inactive-badge" *ngIf="!p.active">Inaktiv</span>
              </div>
              <span class="item-desc" *ngIf="p.description">{{ p.description }}</span>
              <span class="item-prompt">{{ p.promptText | slice:0:120 }}{{ p.promptText.length > 120 ? '...' : '' }}</span>
            </div>
            <div class="item-actions">
              <button class="btn-icon" (click)="editPrompt(p)" title="Bearbeiten">&#9998;</button>
              <button class="btn-icon btn-icon-danger" (click)="deletePrompt(p)" title="Loeschen">&times;</button>
            </div>
          </div>
          <div *ngIf="filteredPrompts.length === 0" class="empty">Keine Prompts gefunden.</div>
        </div>
      </div>
    </div>

    <!-- ===================== CATEGORIES TAB ===================== -->
    <div *ngIf="activeTab === 'categories'">
      <div class="card">
        <div class="section-header">
          <h2 class="section-title">Prompt-Kategorien</h2>
        </div>
        <p class="hint">Kategorien helfen, Prompts zu organisieren. Sie werden im Prompt-Dropdown im Artikeleditor als Gruppen angezeigt.</p>

        <!-- Add category form -->
        <div class="add-row">
          <input type="text" [(ngModel)]="newCategoryName" placeholder="Kategoriename" class="add-input">
          <input type="text" [(ngModel)]="newCategoryDescription" placeholder="Beschreibung (optional)" class="add-input">
          <button class="btn btn-primary btn-sm" (click)="addCategory()" [disabled]="!newCategoryName.trim()">+ Hinzufuegen</button>
        </div>

        <!-- Edit category inline -->
        <div class="item-list">
          <div *ngFor="let c of promptCategories" class="item">
            <div class="item-info" *ngIf="editingCategoryId !== c.id">
              <span class="item-name">{{ c.name }}</span>
              <span class="item-desc" *ngIf="c.description">{{ c.description }}</span>
            </div>
            <div class="edit-inline" *ngIf="editingCategoryId === c.id">
              <input type="text" [(ngModel)]="editCategoryName" class="edit-input">
              <input type="text" [(ngModel)]="editCategoryDescription" class="edit-input" placeholder="Beschreibung">
              <button class="btn btn-primary btn-sm" (click)="saveCategory(c.id)">Speichern</button>
              <button class="btn btn-secondary btn-sm" (click)="editingCategoryId = ''">Abbrechen</button>
            </div>
            <div class="item-actions" *ngIf="editingCategoryId !== c.id">
              <button class="btn-icon" (click)="startEditCategory(c)" title="Bearbeiten">&#9998;</button>
              <button class="btn-icon btn-icon-danger" (click)="deleteCategory(c)" title="Loeschen">&times;</button>
            </div>
          </div>
          <div *ngIf="promptCategories.length === 0" class="empty">Keine Prompt-Kategorien vorhanden.</div>
        </div>
      </div>
    </div>

    <!-- ===================== EXPORT/IMPORT TAB ===================== -->
    <div *ngIf="activeTab === 'exportimport'">
      <div class="sections">
        <div class="card">
          <h2 class="section-title">Daten exportieren</h2>
          <p class="hint">Exportiert alle Daten (Artikel, Kategorien, Tags, Gruppierungen, KI-Prompts) als JSON-Datei.</p>
          <button class="btn btn-primary" (click)="onExport()" [disabled]="exporting">
            {{ exporting ? 'Exportiert...' : 'Alle Daten exportieren' }}
          </button>
        </div>

        <div class="card">
          <h2 class="section-title">Daten importieren</h2>
          <p class="hint">Importiert Daten aus einer zuvor exportierten JSON-Datei.</p>
          <div class="import-area">
            <input type="file" accept=".json" (change)="onFileSelected($event)" #fileInput style="display:none">
            <button class="btn btn-secondary" (click)="fileInput.click()" [disabled]="importing">Datei auswaehlen...</button>
            <span class="file-name" *ngIf="selectedFileName">{{ selectedFileName }}</span>
          </div>
          <button class="btn btn-primary" style="margin-top:1rem" (click)="onImport()"
                  [disabled]="!selectedFile || importing">
            {{ importing ? 'Importiert...' : 'Daten importieren' }}
          </button>
          <div *ngIf="importResult" class="import-result">
            <h3>Import abgeschlossen</h3>
            <ul>
              <li>{{ importResult.articlesImported }} Artikel importiert</li>
              <li>{{ importResult.categoriesImported }} Kategorien importiert, {{ importResult.categoriesSkipped }} uebersprungen</li>
              <li>{{ importResult.tagsImported }} Tags importiert, {{ importResult.tagsSkipped }} uebersprungen</li>
              <li>{{ importResult.groupingsImported }} Gruppierungen importiert, {{ importResult.groupingsSkipped }} uebersprungen</li>
              <li>{{ importResult.promptsImported }} Prompts importiert</li>
            </ul>
          </div>
          <div *ngIf="importError" class="import-error">{{ importError }}</div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .page-header { margin-bottom: 1rem; }
    .page-header h1 { font-size: 1.25rem; font-weight: 600; }
    .subtitle { font-size: 0.8125rem; color: #6b7280; margin-top: 0.25rem; }
    .tab-bar { display: flex; gap: 0; margin-bottom: 1.5rem; border-bottom: 2px solid #e5e7eb; }
    .tab { padding: 0.625rem 1.25rem; border: none; background: none; font-size: 0.875rem; font-weight: 500; color: #6b7280; cursor: pointer; border-bottom: 2px solid transparent; margin-bottom: -2px; transition: all 0.15s; }
    .tab:hover { color: #1f2937; }
    .tab.active { color: #006EC7; border-bottom-color: #006EC7; font-weight: 600; }
    .sections { display: flex; flex-direction: column; gap: 1.5rem; }
    .form-card { margin-bottom: 1.5rem; }
    .section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 0.75rem; }
    .section-title { font-size: 1rem; font-weight: 600; margin-bottom: 0; }
    .hint { font-size: 0.75rem; color: #9ca3af; margin-bottom: 1rem; }
    .prompt-form { display: flex; flex-direction: column; gap: 1rem; }
    .form-row { display: flex; gap: 1rem; }
    .form-row .form-group { flex: 1; }
    .form-group-small { max-width: 120px; }
    .form-group label { display: block; font-size: 0.8125rem; font-weight: 500; color: #6b7280; margin-bottom: 0.375rem; }
    .form-group textarea, .form-group input[type="text"], .form-group input[type="number"] { width: 100%; }
    .form-group textarea { resize: vertical; font-family: monospace; font-size: 0.8125rem; }
    .form-actions { display: flex; gap: 0.75rem; justify-content: flex-end; }
    .toggle-label { display: flex; align-items: center; gap: 0.5rem; cursor: pointer; font-size: 0.875rem; margin-top: 0.25rem; }
    .toggle-label input { width: auto; }
    .filter-row { display: flex; gap: 0.75rem; margin-bottom: 1rem; }
    .filter-select { padding: 0.375rem 0.5rem; border: 1px solid #e5e7eb; border-radius: 0.375rem; font-size: 0.8125rem; background: #fff; }
    .btn-sm { font-size: 0.75rem; padding: 0.375rem 0.75rem; }
    .item-list { display: flex; flex-direction: column; gap: 0.5rem; }
    .item { display: flex; align-items: flex-start; gap: 0.75rem; padding: 0.75rem; background: #f9fafb; border-radius: 0.5rem; }
    .item.inactive { opacity: 0.6; }
    .item-info { flex: 1; display: flex; flex-direction: column; gap: 0.25rem; min-width: 0; }
    .item-header { display: flex; align-items: center; gap: 0.5rem; flex-wrap: wrap; }
    .item-name { font-size: 0.875rem; font-weight: 600; color: #1f2937; }
    .item-desc { font-size: 0.75rem; color: #6b7280; }
    .item-prompt { font-size: 0.75rem; color: #9ca3af; font-family: monospace; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .type-badge { padding: 0.1rem 0.4rem; font-size: 0.625rem; font-weight: 600; border-radius: 0.25rem; text-transform: uppercase; }
    .type-badge.summary { background: #dbeafe; color: #1e40af; }
    .type-badge.content { background: #fef3c7; color: #92400e; }
    .category-badge { padding: 0.1rem 0.4rem; font-size: 0.625rem; background: #f3f4f6; color: #374151; border-radius: 0.25rem; }
    .inactive-badge { padding: 0.1rem 0.4rem; font-size: 0.625rem; background: #fef2f2; color: #991b1b; border-radius: 0.25rem; }
    .item-actions { display: flex; gap: 0.25rem; flex-shrink: 0; }
    .btn-icon { border: none; background: none; font-size: 1.125rem; color: #9ca3af; cursor: pointer; padding: 0.25rem; line-height: 1; border-radius: 0.25rem; }
    .btn-icon:hover { background: #f3f4f6; color: #006EC7; }
    .btn-icon-danger:hover { color: #dc2626; }
    .empty { text-align: center; padding: 1.5rem; color: #9ca3af; font-size: 0.875rem; }
    .add-row { display: flex; gap: 0.5rem; margin-bottom: 1rem; align-items: center; }
    .add-input { flex: 1; padding: 0.5rem; border: 1px solid #e5e7eb; border-radius: 0.375rem; font-size: 0.8125rem; }
    .edit-inline { display: flex; gap: 0.5rem; align-items: center; flex: 1; }
    .edit-input { flex: 1; padding: 0.375rem 0.5rem; border: 1px solid #e5e7eb; border-radius: 0.375rem; font-size: 0.8125rem; }
    .import-area { display: flex; align-items: center; gap: 0.75rem; }
    .file-name { font-size: 0.8125rem; color: #374151; }
    .import-result { margin-top: 1rem; padding: 1rem; background: #f0fdf4; border-radius: 0.5rem; border: 1px solid #bbf7d0; }
    .import-result h3 { font-size: 0.875rem; font-weight: 600; color: #166534; margin-bottom: 0.5rem; }
    .import-result ul { list-style: none; padding: 0; margin: 0; }
    .import-result li { font-size: 0.8125rem; color: #166534; padding: 0.125rem 0; }
    .import-error { margin-top: 1rem; padding: 0.75rem 1rem; background: #fef2f2; border: 1px solid #fecaca; border-radius: 0.5rem; color: #991b1b; font-size: 0.8125rem; }
    @media (max-width: 640px) { .form-row { flex-direction: column; } .add-row { flex-direction: column; } }
  `]
})
export class KonfigurationComponent implements OnInit {
  private svc = inject(ArtikelService);

  activeTab: 'prompts' | 'categories' | 'exportimport' = 'prompts';

  // --- Prompt state ---
  allPrompts: PromptConfig[] = [];
  filteredPrompts: PromptConfig[] = [];
  promptFilter = '';
  promptCategoryFilter = '';
  showPromptForm = false;
  editingPrompt: PromptConfig | null = null;
  formName = '';
  formDescription = '';
  formPromptText = '';
  formType: 'SUMMARY' | 'CONTENT' = 'SUMMARY';
  formCategoryId = '';
  formActive = true;
  formSortOrder = 0;
  saving = false;

  // --- Category state ---
  promptCategories: PromptCategory[] = [];
  newCategoryName = '';
  newCategoryDescription = '';
  editingCategoryId = '';
  editCategoryName = '';
  editCategoryDescription = '';

  // --- Export/Import state ---
  exporting = false;
  importing = false;
  selectedFile: File | null = null;
  selectedFileName = '';
  importResult: any = null;
  importError = '';

  ngOnInit(): void {
    this.loadPrompts();
    this.loadCategories();
  }

  // === PROMPTS ===

  loadPrompts(): void {
    this.svc.listPrompts().subscribe({
      next: p => {
        this.allPrompts = p;
        this.filterPrompts();
      }
    });
  }

  filterPrompts(): void {
    this.filteredPrompts = this.allPrompts.filter(p => {
      if (this.promptFilter && p.promptType !== this.promptFilter) return false;
      if (this.promptCategoryFilter && p.categoryId !== this.promptCategoryFilter) return false;
      return true;
    });
  }

  openPromptForm(type: 'SUMMARY' | 'CONTENT' = 'SUMMARY'): void {
    this.editingPrompt = null;
    this.formName = '';
    this.formDescription = '';
    this.formPromptText = '';
    this.formType = type;
    this.formCategoryId = '';
    this.formActive = true;
    this.formSortOrder = 0;
    this.showPromptForm = true;
  }

  editPrompt(p: PromptConfig): void {
    this.editingPrompt = p;
    this.formName = p.name;
    this.formDescription = p.description || '';
    this.formPromptText = p.promptText;
    this.formType = p.promptType;
    this.formCategoryId = p.categoryId || '';
    this.formActive = p.active;
    this.formSortOrder = p.sortOrder;
    this.showPromptForm = true;
  }

  cancelPromptForm(): void {
    this.showPromptForm = false;
    this.editingPrompt = null;
  }

  savePrompt(): void {
    if (!this.formName.trim() || !this.formPromptText.trim()) return;
    this.saving = true;

    const data: any = {
      name: this.formName.trim(),
      description: this.formDescription.trim() || null,
      promptText: this.formPromptText,
      promptType: this.formType,
      categoryId: this.formCategoryId || null,
      active: this.formActive,
      sortOrder: this.formSortOrder
    };

    const obs = this.editingPrompt
        ? this.svc.updatePrompt(this.editingPrompt.id, data)
        : this.svc.createPrompt(data);

    obs.subscribe({
      next: () => {
        this.saving = false;
        this.showPromptForm = false;
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

  // === CATEGORIES ===

  loadCategories(): void {
    this.svc.listPromptCategories().subscribe({
      next: c => this.promptCategories = c
    });
  }

  addCategory(): void {
    if (!this.newCategoryName.trim()) return;
    this.svc.createPromptCategory({
      name: this.newCategoryName.trim(),
      description: this.newCategoryDescription.trim() || null
    }).subscribe({
      next: () => {
        this.newCategoryName = '';
        this.newCategoryDescription = '';
        this.loadCategories();
      }
    });
  }

  startEditCategory(c: PromptCategory): void {
    this.editingCategoryId = c.id;
    this.editCategoryName = c.name;
    this.editCategoryDescription = c.description || '';
  }

  saveCategory(id: string): void {
    this.svc.updatePromptCategory(id, {
      name: this.editCategoryName.trim(),
      description: this.editCategoryDescription.trim() || null
    }).subscribe({
      next: () => {
        this.editingCategoryId = '';
        this.loadCategories();
        this.loadPrompts();
      }
    });
  }

  deleteCategory(c: PromptCategory): void {
    if (!confirm(`Kategorie "${c.name}" wirklich loeschen?`)) return;
    this.svc.deletePromptCategory(c.id).subscribe({
      next: () => {
        this.loadCategories();
        this.loadPrompts();
      }
    });
  }

  // === EXPORT / IMPORT ===

  onExport(): void {
    this.exporting = true;
    this.svc.exportAll().subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'wissensmanagement-export.json';
        a.click();
        URL.revokeObjectURL(url);
        this.exporting = false;
      },
      error: () => {
        this.exporting = false;
        alert('Export fehlgeschlagen.');
      }
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];
      this.selectedFileName = this.selectedFile.name;
      this.importResult = null;
      this.importError = '';
    }
  }

  onImport(): void {
    if (!this.selectedFile) return;
    if (!confirm('Daten importieren? Bestehende Daten bleiben erhalten.')) return;

    this.importing = true;
    this.importResult = null;
    this.importError = '';

    const reader = new FileReader();
    reader.onload = () => {
      try {
        const data = JSON.parse(reader.result as string);
        this.svc.importAll(data).subscribe({
          next: (result) => {
            this.importResult = result;
            this.importing = false;
            this.selectedFile = null;
            this.selectedFileName = '';
            this.loadPrompts();
            this.loadCategories();
          },
          error: (err) => {
            this.importing = false;
            this.importError = err.error?.error || 'Import fehlgeschlagen.';
          }
        });
      } catch {
        this.importing = false;
        this.importError = 'Ungueltige JSON-Datei.';
      }
    };
    reader.readAsText(this.selectedFile);
  }
}
