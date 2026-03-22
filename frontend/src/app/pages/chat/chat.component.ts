import { Component, inject, OnInit, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ChatService, ChatSessionDto, LlmModelDto, SourceRef, RetrievalTraceInfo } from '../../services/chat.service';

interface DisplayMessage {
  role: string;
  content: string;
  sources?: SourceRef[];
  model?: string | null;
  retrievalTrace?: RetrievalTraceInfo[] | null;
}

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="chat-layout">
      <!-- Sidebar: Sessions -->
      <div class="sessions-panel" [class.collapsed]="sessionsPanelCollapsed">
        <div class="sessions-header">
          <h2 *ngIf="!sessionsPanelCollapsed">Unterhaltungen</h2>
          <button class="btn-icon" (click)="sessionsPanelCollapsed = !sessionsPanelCollapsed"
                  [title]="sessionsPanelCollapsed ? 'Einblenden' : 'Ausblenden'">
            {{ sessionsPanelCollapsed ? '&#9654;' : '&#9664;' }}
          </button>
        </div>

        <div *ngIf="!sessionsPanelCollapsed">
          <button class="btn btn-primary new-chat-btn" (click)="newSession()">+ Neue Unterhaltung</button>

          <div class="session-list">
            <div *ngFor="let s of sessions" class="session-item" [class.active]="s.id === currentSessionId"
                 (click)="selectSession(s)">
              <span class="session-title">{{ s.title }}</span>
              <button class="btn-delete" (click)="deleteSession(s, $event)" title="Loeschen">&times;</button>
            </div>
            <div *ngIf="sessions.length === 0" class="sessions-empty">Keine Unterhaltungen</div>
          </div>
        </div>
      </div>

      <!-- Main Chat -->
      <div class="chat-main">
        <div class="chat-header">
          <h1>{{ currentSessionTitle || 'Wissenschat' }}</h1>
          <select *ngIf="llmModels.length > 0" class="model-select" [(ngModel)]="selectedModelId"
                  title="LLM-Modell waehlen">
            <option value="">Standard-Modell</option>
            <option *ngFor="let m of llmModels" [value]="m.id">
              {{ m.name || m.model }} ({{ m.provider }}){{ m.isActive ? ' *' : '' }}
            </option>
          </select>
          <span *ngIf="lastModel" class="model-badge">{{ lastModel }}</span>
          <button class="btn-icon debug-toggle" (click)="showDebug = !showDebug"
                  [title]="showDebug ? 'Debug ausblenden' : 'Debug einblenden'"
                  [class.active]="showDebug">&#9881;</button>
        </div>

        <div class="messages-area" #messagesContainer>
          <div *ngIf="messages.length === 0 && !loading" class="empty-state">
            <div class="empty-icon">&#128172;</div>
            <h3>Willkommen im Wissenschat</h3>
            <p>Stellen Sie Fragen zu Ihrer Wissensdatenbank und erhalten Sie KI-gestuetzte Antworten auf Basis Ihrer Artikel.</p>
          </div>

          <div *ngFor="let m of messages" class="message" [class.user]="m.role === 'user'" [class.assistant]="m.role === 'assistant'">
            <div class="message-bubble">
              <div class="message-content" [style.white-space]="'pre-wrap'">{{ m.content }}</div>
              <div *ngIf="m.sources && m.sources.length > 0" class="sources">
                <span class="sources-label">Quellen:</span>
                <div class="source-list">
                  <a *ngFor="let s of m.sources; let i = index" [routerLink]="'/artikel/' + s.articleId" class="source-link">
                    <span class="source-num">[{{ i + 1 }}]</span>
                    {{ s.title }}<span *ngIf="s.categoryName" class="source-cat"> &ndash; {{ s.categoryName }}</span>
                  </a>
                </div>
              </div>
              <div *ngIf="showDebug && m.retrievalTrace && m.retrievalTrace.length > 0" class="trace-info">
                <span class="trace-label" (click)="traceExpanded = !traceExpanded">
                  Retrieval-Details {{ traceExpanded ? '&#9660;' : '&#9654;' }}
                </span>
                <div *ngIf="traceExpanded" class="trace-details">
                  <div *ngFor="let t of m.retrievalTrace" class="trace-item">
                    <span class="trace-strategy">{{ t.strategy }}</span>
                    <span class="trace-count">{{ t.resultCount }} Treffer</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div *ngIf="loading" class="message assistant">
            <div class="message-bubble typing">
              <span class="dot"></span><span class="dot"></span><span class="dot"></span>
            </div>
          </div>
        </div>

        <div *ngIf="errorMsg" class="error-bar">{{ errorMsg }}</div>

        <div class="input-area">
          <input type="text" [(ngModel)]="userInput" (keyup.enter)="send()"
                 placeholder="Stellen Sie eine Frage..." [disabled]="loading">
          <button class="btn btn-primary" (click)="send()" [disabled]="!userInput.trim() || loading">Senden</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .chat-layout { display: flex; height: calc(100vh - 5rem); gap: 0; }
    .sessions-panel { width: 260px; background: #fff; border-right: 1px solid #e5e7eb; display: flex; flex-direction: column; transition: width 0.2s; flex-shrink: 0; }
    .sessions-panel.collapsed { width: 40px; }
    .sessions-header { display: flex; align-items: center; justify-content: space-between; padding: 0.75rem; border-bottom: 1px solid #e5e7eb; }
    .sessions-header h2 { font-size: 0.875rem; font-weight: 600; }
    .btn-icon { border: none; background: none; cursor: pointer; font-size: 0.75rem; color: #6b7280; padding: 0.25rem; }
    .btn-icon:hover { color: #006EC7; }
    .new-chat-btn { width: calc(100% - 1.5rem); margin: 0.75rem; font-size: 0.8125rem; }
    .session-list { flex: 1; overflow-y: auto; }
    .session-item { display: flex; align-items: center; justify-content: space-between; padding: 0.625rem 0.75rem; cursor: pointer; border-bottom: 1px solid #f3f4f6; font-size: 0.8125rem; }
    .session-item:hover { background: #f9fafb; }
    .session-item.active { background: #eff6ff; border-left: 3px solid #006EC7; }
    .session-title { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .btn-delete { border: none; background: none; color: #d1d5db; cursor: pointer; font-size: 1rem; padding: 0; line-height: 1; }
    .btn-delete:hover { color: #dc2626; }
    .sessions-empty { text-align: center; padding: 1.5rem 0.75rem; color: #9ca3af; font-size: 0.8125rem; }
    .chat-main { flex: 1; display: flex; flex-direction: column; min-width: 0; background: #fafafa; }
    .chat-header { display: flex; align-items: center; gap: 0.75rem; padding: 0.75rem 1.5rem; border-bottom: 1px solid #e5e7eb; background: #fff; }
    .chat-header h1 { font-size: 1rem; font-weight: 600; flex: 1; }
    .model-select { font-size: 0.75rem; padding: 0.25rem 0.5rem; border: 1px solid #e5e7eb; border-radius: 0.375rem; background: #f9fafb; color: #374151; max-width: 200px; }
    .model-badge { font-size: 0.6875rem; padding: 0.15rem 0.5rem; background: #f3f4f6; border-radius: 0.25rem; color: #6b7280; }
    .messages-area { flex: 1; overflow-y: auto; padding: 1.5rem; display: flex; flex-direction: column; gap: 1rem; }
    .empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; text-align: center; color: #9ca3af; }
    .empty-icon { font-size: 3rem; margin-bottom: 1rem; }
    .empty-state h3 { font-size: 1.125rem; font-weight: 600; color: #6b7280; margin-bottom: 0.5rem; }
    .empty-state p { font-size: 0.875rem; max-width: 400px; line-height: 1.6; }
    .message { display: flex; }
    .message.user { justify-content: flex-end; }
    .message.assistant { justify-content: flex-start; }
    .message-bubble { max-width: 75%; padding: 0.75rem 1rem; border-radius: 1rem; font-size: 0.875rem; line-height: 1.6; }
    .message.user .message-bubble { background: #006EC7; color: #fff; border-bottom-right-radius: 0.25rem; }
    .message.assistant .message-bubble { background: #fff; color: #1f2937; border-bottom-left-radius: 0.25rem; box-shadow: 0 1px 3px rgba(0,0,0,0.08); }
    .typing { display: flex; gap: 0.25rem; padding: 0.75rem 1.25rem; }
    .dot { width: 0.5rem; height: 0.5rem; border-radius: 50%; background: #9ca3af; animation: bounce 1.4s infinite ease-in-out both; }
    .dot:nth-child(1) { animation-delay: -0.32s; }
    .dot:nth-child(2) { animation-delay: -0.16s; }
    @keyframes bounce { 0%, 80%, 100% { transform: scale(0); } 40% { transform: scale(1); } }
    .sources { margin-top: 0.5rem; padding-top: 0.5rem; border-top: 1px solid #e5e7eb; }
    .sources-label { font-size: 0.75rem; color: #6b7280; display: block; margin-bottom: 0.25rem; font-weight: 500; }
    .source-list { display: flex; flex-direction: column; gap: 0.125rem; }
    .source-link { font-size: 0.75rem; padding: 0.25rem 0.5rem; background: #eff6ff; border-radius: 0.25rem; color: #006EC7; text-decoration: none; display: block; }
    .source-link:hover { background: #dbeafe; }
    .source-num { font-weight: 600; margin-right: 0.25rem; }
    .source-cat { color: #9ca3af; font-size: 0.6875rem; }
    .error-bar { padding: 0.5rem 1.5rem; background: #fef2f2; color: #dc2626; font-size: 0.8125rem; border-top: 1px solid #fecaca; }
    .input-area { display: flex; gap: 0.75rem; padding: 1rem 1.5rem; border-top: 1px solid #e5e7eb; background: #fff; }
    .input-area input { flex: 1; }
    .debug-toggle { font-size: 1rem; opacity: 0.4; transition: opacity 0.15s; }
    .debug-toggle:hover, .debug-toggle.active { opacity: 1; color: #006EC7; }
    .trace-info { margin-top: 0.375rem; padding-top: 0.375rem; border-top: 1px dashed #e5e7eb; }
    .trace-label { font-size: 0.6875rem; color: #9ca3af; cursor: pointer; user-select: none; }
    .trace-label:hover { color: #6b7280; }
    .trace-details { margin-top: 0.25rem; display: flex; flex-direction: column; gap: 0.125rem; }
    .trace-item { display: flex; gap: 0.5rem; font-size: 0.625rem; color: #9ca3af; }
    .trace-strategy { background: #f3f4f6; padding: 0.05rem 0.3rem; border-radius: 0.2rem; font-family: monospace; }
    .trace-count { color: #6b7280; }
    @media (max-width: 768px) { .sessions-panel { display: none; } }
  `]
})
export class ChatComponent implements OnInit, AfterViewChecked {
  private chatSvc = inject(ChatService);
  @ViewChild('messagesContainer') private messagesContainer!: ElementRef;

  sessions: ChatSessionDto[] = [];
  llmModels: LlmModelDto[] = [];
  selectedModelId = '';
  currentSessionId: string | null = null;
  currentSessionTitle = '';
  messages: DisplayMessage[] = [];
  userInput = '';
  loading = false;
  errorMsg = '';
  lastModel: string | null = null;
  sessionsPanelCollapsed = false;
  showDebug = false;
  traceExpanded = false;
  private shouldScroll = false;

  ngOnInit(): void {
    this.loadSessions();
    this.chatSvc.listLlmModels().subscribe({
      next: models => this.llmModels = models,
      error: () => {}
    });
  }

  ngAfterViewChecked(): void {
    if (this.shouldScroll) {
      this.scrollToBottom();
      this.shouldScroll = false;
    }
  }

  loadSessions(): void {
    this.chatSvc.listSessions().subscribe({
      next: s => this.sessions = s,
      error: () => {}
    });
  }

  selectSession(session: ChatSessionDto): void {
    this.currentSessionId = session.id;
    this.currentSessionTitle = session.title;
    this.messages = [];
    this.errorMsg = '';

    this.chatSvc.getMessages(session.id).subscribe({
      next: msgs => {
        this.messages = msgs.map(m => {
          const dm: DisplayMessage = { role: m.role, content: m.content };
          if (m.sourceRefs) {
            try { dm.sources = JSON.parse(m.sourceRefs); } catch {}
          }
          if (m.modelId) dm.model = m.modelId;
          return dm;
        });
        this.shouldScroll = true;
      }
    });
  }

  newSession(): void {
    this.currentSessionId = null;
    this.currentSessionTitle = '';
    this.messages = [];
    this.errorMsg = '';
    this.lastModel = null;
  }

  deleteSession(session: ChatSessionDto, event: Event): void {
    event.stopPropagation();
    if (!confirm(`Unterhaltung "${session.title}" loeschen?`)) return;
    this.chatSvc.deleteSession(session.id).subscribe({
      next: () => {
        this.sessions = this.sessions.filter(s => s.id !== session.id);
        if (this.currentSessionId === session.id) {
          this.newSession();
        }
      }
    });
  }

  send(): void {
    const text = this.userInput.trim();
    if (!text || this.loading) return;

    this.messages.push({ role: 'user', content: text });
    this.userInput = '';
    this.loading = true;
    this.errorMsg = '';
    this.shouldScroll = true;

    this.chatSvc.send(this.currentSessionId, text, this.selectedModelId || null).subscribe({
      next: response => {
        this.currentSessionId = response.sessionId;
        this.currentSessionTitle = response.sessionTitle;
        this.lastModel = response.model;

        this.messages.push({
          role: 'assistant',
          content: response.content,
          sources: response.sources,
          model: response.model,
          retrievalTrace: response.retrievalTrace
        });

        this.loading = false;
        this.shouldScroll = true;
        this.loadSessions();
      },
      error: err => {
        this.loading = false;
        this.errorMsg = err.error?.error || err.error?.message || 'Fehler bei der Verarbeitung. Bitte versuchen Sie es erneut.';
      }
    });
  }

  private scrollToBottom(): void {
    try {
      const el = this.messagesContainer?.nativeElement;
      if (el) el.scrollTop = el.scrollHeight;
    } catch {}
  }
}
