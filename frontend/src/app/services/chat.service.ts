import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ChatSessionDto {
  id: string;
  title: string;
  modelConfigId: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ChatMessageDto {
  id: string;
  role: string;
  content: string;
  sourceRefs: string | null;
  modelId: string | null;
  tokenCount: number | null;
  createdAt: string;
}

export interface SourceRef {
  articleId: string;
  title: string;
  categoryName: string | null;
}

export interface ChatResponseDto {
  sessionId: string;
  sessionTitle: string;
  content: string;
  sources: SourceRef[];
  model: string | null;
  tokenCount: number;
}

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly base = 'api/chat';

  constructor(private http: HttpClient) {}

  listSessions(): Observable<ChatSessionDto[]> {
    return this.http.get<ChatSessionDto[]>(`${this.base}/sessions`);
  }

  createSession(title?: string): Observable<ChatSessionDto> {
    return this.http.post<ChatSessionDto>(`${this.base}/sessions`, { title });
  }

  deleteSession(sessionId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/sessions/${sessionId}`);
  }

  getMessages(sessionId: string): Observable<ChatMessageDto[]> {
    return this.http.get<ChatMessageDto[]>(`${this.base}/sessions/${sessionId}/messages`);
  }

  send(sessionId: string | null, message: string): Observable<ChatResponseDto> {
    return this.http.post<ChatResponseDto>(`${this.base}/send`, { sessionId, message });
  }
}
