import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ArtikelService } from '../../services/artikel.service';
import { DocumentSuggestion, LlmModelConfig } from '../../models/artikel.model';

@Component({
  selector: 'app-dokument-kodierung',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page-header">
      <h1>KI-Kodierempfehlung fuer Dokumente</h1>
      <p class="subtitle">Laden Sie Dokumente hoch, um automatisierte Kodierempfehlungen zu erhalten.</p>
    </div>

    <!-- Upload-Bereich -->
    <div class="card upload-card">
      <h2>Dokument hochladen</h2>
      <div class="upload-row">
        <div class="upload-area" (click)="fileInput.click()"
             (dragover)="onDragOver($event)" (drop)="onDrop($event)"
             [class.dragover]="isDragOver">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="upload-icon">
            <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/>
            <polyline points="17 8 12 3 7 8"/>
            <line x1="12" y1="3" x2="12" y2="15"/>
          </svg>
          <span class="upload-text">Datei hierher ziehen oder klicken</span>
          <span class="upload-hint">PDF, TXT oder andere Textdokumente</span>
          <input #fileInput type="file" accept=".pdf,.txt,.csv,.doc,.docx" (change)="onFileSelected($event)" style="display:none" multiple>
        </div>
        <div class="upload-options">
          <label>LLM-Modell (optional)</label>
          <select [(ngModel)]="selectedModelConfigId" class="form-input">
            <option [ngValue]="null">Standard</option>
            <option *ngFor="let m of llmModels" [ngValue]="m.id">{{ m.name }}</option>
          </select>
        </div>
      </div>
    </div>

    <!-- Datei-Liste -->
    <div class="card" *ngIf="documents.length > 0">
      <div class="list-header">
        <h2>Hochgeladene Dokumente</h2>
        <button class="btn btn-secondary btn-sm" (click)="refreshList()" [disabled]="refreshing">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:14px;height:14px;margin-right:4px">
            <polyline points="23 4 23 10 17 10"/>
            <path d="M20.49 15a9 9 0 11-2.12-9.36L23 10"/>
          </svg>
          Aktualisieren
        </button>
      </div>

      <div class="doc-list">
        <div class="doc-item" *ngFor="let doc of documents" [class.doc-completed]="doc.status === 'COMPLETED'" [class.doc-error]="doc.status === 'ERROR'">
          <div class="doc-info">
            <div class="doc-name">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="doc-icon">
                <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
                <polyline points="14 2 14 8 20 8"/>
              </svg>
              {{ doc.fileName }}
            </div>
            <div class="doc-meta">
              <span class="doc-date">{{ doc.createdAt | date:'dd.MM.yyyy HH:mm' }}</span>
              <span class="status-badge" [class]="'status-' + doc.status.toLowerCase()">
                {{ statusLabel(doc.status) }}
              </span>
            </div>
            <div class="doc-error-msg" *ngIf="doc.status === 'ERROR' && doc.errorMessage">
              {{ doc.errorMessage }}
            </div>
          </div>
          <div class="doc-actions">
            <button class="btn btn-primary btn-sm"
                    *ngIf="doc.status === 'PENDING'"
                    (click)="startSuggestion(doc)">
              Kodierung starten
            </button>
            <button class="btn btn-primary btn-sm"
                    *ngIf="doc.status === 'ERROR'"
                    (click)="startSuggestion(doc)">
              Erneut versuchen
            </button>
            <button class="btn btn-secondary btn-sm"
                    *ngIf="doc.status === 'COMPLETED'"
                    (click)="showResult(doc)">
              Ergebnis anzeigen
            </button>
            <button class="btn btn-danger btn-sm"
                    (click)="deleteDocument(doc)"
                    [disabled]="doc.status === 'PROCESSING'">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:14px;height:14px">
                <polyline points="3 6 5 6 21 6"/>
                <path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/>
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Ergebnis-Dialog -->
    <div class="overlay" *ngIf="selectedDocument" (click)="selectedDocument = null">
      <div class="result-dialog" (click)="$event.stopPropagation()">
        <div class="dialog-header">
          <h2>Kodierempfehlung: {{ selectedDocument.fileName }}</h2>
          <button class="btn-close" (click)="selectedDocument = null">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
        <div class="dialog-body">
          <div class="result-meta">
            <span *ngIf="selectedDocument.llmModel">Modell: {{ selectedDocument.llmModel }}</span>
            <span *ngIf="selectedDocument.tokenCount">Tokens: {{ selectedDocument.tokenCount }}</span>
          </div>

          <div class="empfehlung-list" *ngIf="selectedDocument.empfehlungen.length > 0">
            <div class="empfehlung-item" *ngFor="let e of selectedDocument.empfehlungen; let i = index">
              <h3>Empfehlung {{ i + 1 }}</h3>
              <div class="empfehlung-text" [innerHTML]="formatText(e)"></div>
            </div>
          </div>

          <div class="quellen-section" *ngIf="selectedDocument.quellen && selectedDocument.quellen.length > 0">
            <h3>Verwendete Quellen</h3>
            <div class="quelle-item" *ngFor="let q of selectedDocument.quellen">
              <span class="quelle-title">{{ q.title }}</span>
              <span class="quelle-binding badge" [class]="'binding-' + q.bindingLevel.toLowerCase()">{{ q.bindingLevel }}</span>
              <span class="quelle-reason">{{ q.matchReason }}</span>
            </div>
          </div>

          <div *ngIf="selectedDocument.empfehlungen.length === 0" class="no-results">
            Keine Empfehlungen vorhanden.
          </div>
        </div>
      </div>
    </div>

    <!-- Leer-Zustand -->
    <div class="card empty-state" *ngIf="documents.length === 0 && !loading">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="empty-icon">
        <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
        <polyline points="14 2 14 8 20 8"/>
        <line x1="16" y1="13" x2="8" y2="13"/>
        <line x1="16" y1="17" x2="8" y2="17"/>
        <polyline points="10 9 9 9 8 9"/>
      </svg>
      <p>Noch keine Dokumente hochgeladen.</p>
      <p class="text-muted">Laden Sie ein Dokument hoch, um eine KI-Kodierempfehlung zu erhalten.</p>
    </div>
  `,
  styles: [`
    .page-header { margin-bottom: 1.5rem; }
    .page-header h1 { font-size: 1.5rem; font-weight: 700; color: #1f2937; margin: 0; }
    .subtitle { color: #6b7280; margin: 0.25rem 0 0; font-size: 0.875rem; }

    .card {
      background: #fff; border: 1px solid #e5e7eb; border-radius: 0.75rem;
      padding: 1.5rem; margin-bottom: 1rem;
    }
    .card h2 { font-size: 1.125rem; font-weight: 600; margin: 0 0 1rem; color: #1f2937; }

    .upload-card .upload-row { display: flex; gap: 1.5rem; align-items: flex-start; }
    .upload-area {
      flex: 1; border: 2px dashed #d1d5db; border-radius: 0.75rem; padding: 2rem;
      text-align: center; cursor: pointer; transition: all 0.2s;
      display: flex; flex-direction: column; align-items: center; gap: 0.5rem;
    }
    .upload-area:hover, .upload-area.dragover { border-color: #006EC7; background: #eff6ff; }
    .upload-icon { width: 40px; height: 40px; color: #9ca3af; }
    .upload-area:hover .upload-icon, .upload-area.dragover .upload-icon { color: #006EC7; }
    .upload-text { font-weight: 600; color: #374151; font-size: 0.9375rem; }
    .upload-hint { color: #9ca3af; font-size: 0.8125rem; }

    .upload-options { min-width: 220px; }
    .upload-options label { display: block; font-size: 0.8125rem; font-weight: 600; color: #374151; margin-bottom: 0.375rem; }

    .form-input {
      width: 100%; padding: 0.5rem 0.75rem; border: 1px solid #d1d5db;
      border-radius: 0.5rem; font-size: 0.875rem; background: #fff;
    }
    .form-input:focus { outline: none; border-color: #006EC7; box-shadow: 0 0 0 3px rgba(0,110,199,0.1); }

    .list-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
    .list-header h2 { margin: 0; }

    .doc-list { display: flex; flex-direction: column; gap: 0.5rem; }
    .doc-item {
      display: flex; justify-content: space-between; align-items: center;
      padding: 0.875rem 1rem; border: 1px solid #e5e7eb; border-radius: 0.5rem;
      transition: all 0.15s;
    }
    .doc-item:hover { border-color: #d1d5db; background: #fafafa; }
    .doc-item.doc-completed { border-left: 3px solid #10b981; }
    .doc-item.doc-error { border-left: 3px solid #ef4444; }

    .doc-info { flex: 1; min-width: 0; }
    .doc-name { display: flex; align-items: center; gap: 0.5rem; font-weight: 600; color: #1f2937; font-size: 0.9375rem; }
    .doc-icon { width: 18px; height: 18px; color: #6b7280; flex-shrink: 0; }
    .doc-meta { display: flex; align-items: center; gap: 0.75rem; margin-top: 0.25rem; font-size: 0.8125rem; }
    .doc-date { color: #9ca3af; }
    .doc-error-msg { color: #ef4444; font-size: 0.8125rem; margin-top: 0.25rem; }

    .status-badge {
      display: inline-block; padding: 0.125rem 0.5rem; border-radius: 9999px;
      font-size: 0.75rem; font-weight: 600;
    }
    .status-pending { background: #fef3c7; color: #92400e; }
    .status-processing { background: #dbeafe; color: #1e40af; animation: pulse 2s infinite; }
    .status-completed { background: #d1fae5; color: #065f46; }
    .status-error { background: #fee2e2; color: #991b1b; }

    @keyframes pulse {
      0%, 100% { opacity: 1; }
      50% { opacity: 0.6; }
    }

    .doc-actions { display: flex; gap: 0.5rem; align-items: center; flex-shrink: 0; margin-left: 1rem; }

    .btn {
      display: inline-flex; align-items: center; gap: 0.25rem;
      border: none; border-radius: 0.5rem; font-weight: 600; cursor: pointer;
      font-size: 0.8125rem; padding: 0.5rem 1rem; transition: all 0.15s;
    }
    .btn:disabled { opacity: 0.5; cursor: not-allowed; }
    .btn-sm { padding: 0.375rem 0.75rem; font-size: 0.8125rem; }
    .btn-primary { background: #006EC7; color: #fff; }
    .btn-primary:hover:not(:disabled) { background: #004a8a; }
    .btn-secondary { background: #f3f4f6; color: #374151; }
    .btn-secondary:hover:not(:disabled) { background: #e5e7eb; }
    .btn-danger { background: #fee2e2; color: #991b1b; }
    .btn-danger:hover:not(:disabled) { background: #fecaca; }

    /* Dialog / Overlay */
    .overlay {
      position: fixed; inset: 0; background: rgba(0,0,0,0.5); z-index: 100;
      display: flex; align-items: center; justify-content: center; padding: 2rem;
    }
    .result-dialog {
      background: #fff; border-radius: 0.75rem; max-width: 800px; width: 100%;
      max-height: 85vh; display: flex; flex-direction: column;
      box-shadow: 0 20px 60px rgba(0,0,0,0.3);
    }
    .dialog-header {
      display: flex; justify-content: space-between; align-items: center;
      padding: 1.25rem 1.5rem; border-bottom: 1px solid #e5e7eb;
    }
    .dialog-header h2 { font-size: 1.125rem; margin: 0; font-weight: 700; }
    .btn-close {
      background: none; border: none; cursor: pointer; padding: 0.25rem;
      color: #9ca3af; border-radius: 0.375rem;
    }
    .btn-close:hover { background: #f3f4f6; color: #1f2937; }
    .btn-close svg { width: 20px; height: 20px; }

    .dialog-body { padding: 1.5rem; overflow-y: auto; flex: 1; }

    .result-meta {
      display: flex; gap: 1rem; margin-bottom: 1.25rem;
      font-size: 0.8125rem; color: #6b7280;
    }

    .empfehlung-list { display: flex; flex-direction: column; gap: 1rem; }
    .empfehlung-item {
      padding: 1rem; background: #f9fafb; border: 1px solid #e5e7eb;
      border-radius: 0.5rem;
    }
    .empfehlung-item h3 { font-size: 0.9375rem; font-weight: 700; color: #006EC7; margin: 0 0 0.5rem; }
    .empfehlung-text { font-size: 0.875rem; line-height: 1.6; color: #374151; white-space: pre-wrap; }

    .quellen-section { margin-top: 1.5rem; }
    .quellen-section h3 { font-size: 1rem; font-weight: 600; margin: 0 0 0.75rem; }
    .quelle-item {
      display: flex; align-items: center; gap: 0.5rem;
      padding: 0.5rem 0; border-bottom: 1px solid #f3f4f6; font-size: 0.8125rem;
    }
    .quelle-title { font-weight: 600; color: #1f2937; }
    .quelle-reason { color: #6b7280; }
    .badge {
      display: inline-block; padding: 0.125rem 0.375rem; border-radius: 0.25rem;
      font-size: 0.6875rem; font-weight: 700;
    }
    .binding-verbindlich { background: #fee2e2; color: #991b1b; }
    .binding-empfehlung { background: #dbeafe; color: #1e40af; }
    .binding-lex_specialis { background: #fef3c7; color: #92400e; }
    .binding-informativ { background: #e5e7eb; color: #374151; }

    .no-results { text-align: center; color: #9ca3af; padding: 2rem; }

    .empty-state { text-align: center; padding: 3rem; }
    .empty-icon { width: 48px; height: 48px; color: #d1d5db; margin-bottom: 1rem; }
    .empty-state p { margin: 0.25rem 0; color: #6b7280; }
    .text-muted { color: #9ca3af !important; font-size: 0.875rem; }
  `]
})
export class DokumentKodierungComponent implements OnInit, OnDestroy {
  private readonly svc = inject(ArtikelService);

  documents: DocumentSuggestion[] = [];
  llmModels: LlmModelConfig[] = [];
  selectedModelConfigId: string | null = null;
  selectedDocument: DocumentSuggestion | null = null;
  loading = false;
  refreshing = false;
  isDragOver = false;

  private pollInterval: any;

  ngOnInit(): void {
    this.loadList();
    this.loadModels();
    // Polling fuer Hintergrund-Updates (alle 5 Sekunden wenn PROCESSING vorhanden)
    this.pollInterval = setInterval(() => {
      if (this.documents.some(d => d.status === 'PROCESSING')) {
        this.refreshList();
      }
    }, 5000);
  }

  ngOnDestroy(): void {
    if (this.pollInterval) clearInterval(this.pollInterval);
  }

  loadList(): void {
    this.loading = true;
    this.svc.listDocumentSuggestions().subscribe({
      next: list => { this.documents = list; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  loadModels(): void {
    // LLM models come from the suggestions endpoint
    this.svc.listDocumentSuggestions().subscribe();
  }

  refreshList(): void {
    this.refreshing = true;
    this.svc.listDocumentSuggestions().subscribe({
      next: list => {
        this.documents = list;
        this.refreshing = false;
        // Update selected document if dialog is open
        if (this.selectedDocument) {
          const updated = list.find(d => d.id === this.selectedDocument!.id);
          if (updated) this.selectedDocument = updated;
        }
      },
      error: () => { this.refreshing = false; }
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      for (let i = 0; i < input.files.length; i++) {
        this.uploadFile(input.files[i]);
      }
      input.value = '';
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver = true;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver = false;
    if (event.dataTransfer?.files) {
      for (let i = 0; i < event.dataTransfer.files.length; i++) {
        this.uploadFile(event.dataTransfer.files[i]);
      }
    }
  }

  private uploadFile(file: File): void {
    this.svc.uploadDocumentSuggestion(file, this.selectedModelConfigId ?? undefined).subscribe({
      next: doc => {
        this.documents.unshift(doc);
      },
      error: err => {
        alert('Upload fehlgeschlagen: ' + (err.error?.error || err.message));
      }
    });
  }

  startSuggestion(doc: DocumentSuggestion): void {
    doc.status = 'PROCESSING';
    this.svc.startDocumentSuggestion(doc.id).subscribe({
      error: err => {
        doc.status = 'ERROR';
        doc.errorMessage = err.error?.error || 'Start fehlgeschlagen';
      }
    });
  }

  deleteDocument(doc: DocumentSuggestion): void {
    if (!confirm('Dokument "' + doc.fileName + '" wirklich loeschen?')) return;
    this.svc.deleteDocumentSuggestion(doc.id).subscribe({
      next: () => {
        this.documents = this.documents.filter(d => d.id !== doc.id);
        if (this.selectedDocument?.id === doc.id) this.selectedDocument = null;
      },
      error: err => {
        alert('Loeschen fehlgeschlagen: ' + (err.error?.error || err.message));
      }
    });
  }

  showResult(doc: DocumentSuggestion): void {
    // Refresh detail data then show
    this.svc.getDocumentSuggestion(doc.id).subscribe({
      next: detail => { this.selectedDocument = detail; },
      error: () => { this.selectedDocument = doc; }
    });
  }

  statusLabel(status: string): string {
    switch (status) {
      case 'PENDING': return 'Ausstehend';
      case 'PROCESSING': return 'In Bearbeitung';
      case 'COMPLETED': return 'Abgeschlossen';
      case 'ERROR': return 'Fehler';
      default: return status;
    }
  }

  formatText(text: string): string {
    return text.replace(/\n/g, '<br>');
  }
}
