import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="page-header">
      <h1>Wissenschat</h1>
    </div>

    <div class="card chat-container">
      <div class="messages-area">
        <div *ngIf="messages.length === 0" class="empty-state">
          <div class="empty-icon">&#128172;</div>
          <h3>Willkommen im Wissenschat</h3>
          <p>Stellen Sie Fragen zu Ihrer Wissensdatenbank und erhalten Sie KI-gestuetzte Antworten auf Basis Ihrer Artikel.</p>
        </div>

        <div *ngFor="let m of messages" class="message" [class.user]="m.role === 'user'" [class.assistant]="m.role === 'assistant'">
          <div class="message-bubble">
            <div class="message-content">{{ m.content }}</div>
            <div *ngIf="m.sources && m.sources.length > 0" class="sources">
              <span class="sources-label">Quellen:</span>
              <a *ngFor="let s of m.sources" [routerLink]="'/artikel/' + s.articleId" class="source-link">{{ s.title }}</a>
            </div>
          </div>
        </div>

        <div *ngIf="loading" class="message assistant">
          <div class="message-bubble typing">
            <span class="dot"></span><span class="dot"></span><span class="dot"></span>
          </div>
        </div>
      </div>

      <div class="input-area">
        <input type="text" [(ngModel)]="userInput" (keyup.enter)="send()" placeholder="Stellen Sie eine Frage..." [disabled]="loading">
        <button class="btn btn-primary" (click)="send()" [disabled]="!userInput.trim() || loading">Senden</button>
      </div>
    </div>
  `,
  styles: [`
    .page-header { margin-bottom: 1rem; }
    .page-header h1 { font-size: 1.25rem; font-weight: 600; }
    .chat-container { display: flex; flex-direction: column; height: calc(100vh - 10rem); padding: 0; overflow: hidden; }
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
    .message.assistant .message-bubble { background: #f3f4f6; color: #1f2937; border-bottom-left-radius: 0.25rem; }
    .typing { display: flex; gap: 0.25rem; padding: 0.75rem 1.25rem; }
    .dot { width: 0.5rem; height: 0.5rem; border-radius: 50%; background: #9ca3af; animation: bounce 1.4s infinite ease-in-out both; }
    .dot:nth-child(1) { animation-delay: -0.32s; }
    .dot:nth-child(2) { animation-delay: -0.16s; }
    @keyframes bounce { 0%, 80%, 100% { transform: scale(0); } 40% { transform: scale(1); } }
    .sources { margin-top: 0.5rem; padding-top: 0.5rem; border-top: 1px solid #e5e7eb; }
    .sources-label { font-size: 0.75rem; color: #6b7280; margin-right: 0.375rem; }
    .source-link { font-size: 0.75rem; margin-right: 0.5rem; color: #006EC7; }
    .input-area { display: flex; gap: 0.75rem; padding: 1rem 1.5rem; border-top: 1px solid #e5e7eb; background: #fff; }
    .input-area input { flex: 1; }
  `]
})
export class ChatComponent {
  messages: { role: string; content: string; sources?: { articleId: string; title: string }[] }[] = [];
  userInput = '';
  loading = false;

  send(): void {
    const text = this.userInput.trim();
    if (!text || this.loading) return;

    this.messages.push({ role: 'user', content: text });
    this.userInput = '';
    this.loading = true;

    // Placeholder - will be connected to backend chat API in Iteration 5
    setTimeout(() => {
      this.messages.push({
        role: 'assistant',
        content: 'Die Chat-Funktion wird in einem kommenden Update verfuegbar sein. Bitte nutzen Sie in der Zwischenzeit die Suchfunktion in der Wissensdatenbank.'
      });
      this.loading = false;
    }, 1000);
  }
}
