import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Article, ArticlePage, ArticleVersion, ArticleTreeNode, BreadcrumbItem, Category, DocumentSuggestion, Grouping, PromptConfig, PromptCategory, SearchResult, StructuredResult, Tag, Statistik } from '../models/artikel.model';

@Injectable({ providedIn: 'root' })
export class ArtikelService {
  private readonly base = 'api/artikel';
  private readonly catBase = 'api/kategorien';
  private readonly tagBase = 'api/tags';
  private readonly dictBase = 'api/diktat';

  constructor(private http: HttpClient) {}

  // --- Article CRUD ---

  list(params: {
    status?: string; q?: string; categoryId?: string; groupingId?: string;
    page?: number; size?: number; sortBy?: string; sortDir?: string;
  } = {}): Observable<ArticlePage> {
    let p = new HttpParams();
    if (params.status) p = p.set('status', params.status);
    if (params.q) p = p.set('q', params.q);
    if (params.categoryId) p = p.set('categoryId', params.categoryId);
    if (params.groupingId) p = p.set('groupingId', params.groupingId);
    p = p.set('page', String(params.page ?? 0));
    p = p.set('size', String(params.size ?? 20));
    if (params.sortBy) p = p.set('sortBy', params.sortBy);
    if (params.sortDir) p = p.set('sortDir', params.sortDir);
    return this.http.get<ArticlePage>(this.base, { params: p });
  }

  getById(id: string, trackView = true): Observable<Article> {
    return this.http.get<Article>(`${this.base}/${id}?trackView=${trackView}`);
  }

  create(data: any): Observable<Article> {
    return this.http.post<Article>(this.base, data);
  }

  update(id: string, data: any): Observable<Article> {
    return this.http.put<Article>(`${this.base}/${id}`, data);
  }

  publish(id: string): Observable<Article> {
    return this.http.put<Article>(`${this.base}/${id}/publish`, {});
  }

  archive(id: string): Observable<Article> {
    return this.http.put<Article>(`${this.base}/${id}/archive`, {});
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  getVersions(id: string): Observable<ArticleVersion[]> {
    return this.http.get<ArticleVersion[]>(`${this.base}/${id}/versionen`);
  }

  submitFeedback(id: string, rating: number, comment?: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${id}/feedback`, { rating, comment });
  }

  getStatistik(): Observable<Statistik> {
    return this.http.get<Statistik>(`${this.base}/statistik`);
  }

  getNewest(limit = 5): Observable<Article[]> {
    return this.http.get<Article[]>(`${this.base}/neueste?limit=${limit}`);
  }

  getPopular(limit = 5): Observable<Article[]> {
    return this.http.get<Article[]>(`${this.base}/beliebt?limit=${limit}`);
  }

  // --- Hierarchy ---

  getTree(): Observable<ArticleTreeNode[]> {
    return this.http.get<ArticleTreeNode[]>(`${this.base}/baum`);
  }

  getChildren(parentId: string): Observable<Article[]> {
    return this.http.get<Article[]>(`${this.base}/${parentId}/kinder`);
  }

  getBreadcrumb(id: string): Observable<BreadcrumbItem[]> {
    return this.http.get<BreadcrumbItem[]>(`${this.base}/${id}/breadcrumb`);
  }

  moveArticle(id: string, newParentId: string | null): Observable<void> {
    return this.http.put<void>(`${this.base}/${id}/verschieben`, { newParentId });
  }

  reorderArticles(parentArticleId: string | null, orderedIds: string[]): Observable<void> {
    return this.http.put<void>(`${this.base}/sortierung`, { parentArticleId, orderedIds });
  }

  // --- Search ---

  search(query: string, mode: string = 'HYBRID', limit: number = 20): Observable<SearchResult[]> {
    let p = new HttpParams().set('q', query).set('mode', mode).set('limit', String(limit));
    return this.http.get<SearchResult[]>(`${this.base}/suche`, { params: p });
  }

  // --- Categories & Tags ---

  listCategories(): Observable<Category[]> {
    return this.http.get<Category[]>(this.catBase);
  }

  createCategory(data: Partial<Category>): Observable<Category> {
    return this.http.post<Category>(this.catBase, data);
  }

  deleteCategory(id: string): Observable<void> {
    return this.http.delete<void>(`${this.catBase}/${id}`);
  }

  listTags(): Observable<Tag[]> {
    return this.http.get<Tag[]>(this.tagBase);
  }

  createTag(name: string): Observable<Tag> {
    return this.http.post<Tag>(this.tagBase, { name });
  }

  deleteTag(id: string): Observable<void> {
    return this.http.delete<void>(`${this.tagBase}/${id}`);
  }

  listGroupings(): Observable<Grouping[]> {
    return this.http.get<Grouping[]>('api/gruppierungen');
  }

  createGrouping(name: string, description?: string): Observable<Grouping> {
    return this.http.post<Grouping>('api/gruppierungen', { name, description });
  }

  deleteGrouping(id: string): Observable<void> {
    return this.http.delete<void>(`api/gruppierungen/${id}`);
  }

  generateSummary(title: string, content: string): Observable<{ summary: string }> {
    return this.http.post<{ summary: string }>(`${this.base}/generate-summary`, { title, content });
  }

  structureText(content: string): Observable<StructuredResult> {
    return this.http.post<StructuredResult>(`${this.base}/structure`, { content });
  }

  // --- Prompt Configs ---

  private readonly promptBase = 'api/prompts';

  listPrompts(type?: 'SUMMARY' | 'CONTENT', active?: boolean): Observable<PromptConfig[]> {
    let p = new HttpParams();
    if (type) p = p.set('type', type);
    if (active !== undefined) p = p.set('active', String(active));
    return this.http.get<PromptConfig[]>(this.promptBase, { params: p });
  }

  getPrompt(id: string): Observable<PromptConfig> {
    return this.http.get<PromptConfig>(`${this.promptBase}/${id}`);
  }

  createPrompt(data: Partial<PromptConfig>): Observable<PromptConfig> {
    return this.http.post<PromptConfig>(this.promptBase, data);
  }

  updatePrompt(id: string, data: Partial<PromptConfig>): Observable<PromptConfig> {
    return this.http.put<PromptConfig>(`${this.promptBase}/${id}`, data);
  }

  // --- Prompt Categories ---

  private readonly promptCatBase = 'api/prompt-kategorien';

  listPromptCategories(): Observable<PromptCategory[]> {
    return this.http.get<PromptCategory[]>(this.promptCatBase);
  }

  createPromptCategory(data: Partial<PromptCategory>): Observable<PromptCategory> {
    return this.http.post<PromptCategory>(this.promptCatBase, data);
  }

  updatePromptCategory(id: string, data: Partial<PromptCategory>): Observable<PromptCategory> {
    return this.http.put<PromptCategory>(`${this.promptCatBase}/${id}`, data);
  }

  deletePromptCategory(id: string): Observable<void> {
    return this.http.delete<void>(`${this.promptCatBase}/${id}`);
  }

  deletePrompt(id: string): Observable<void> {
    return this.http.delete<void>(`${this.promptBase}/${id}`);
  }

  applyPrompt(promptId: string, content: string, title?: string): Observable<{ result: string }> {
    return this.http.post<{ result: string }>(`${this.promptBase}/${promptId}/anwenden`, { content, title });
  }

  // --- Export / Import ---

  private readonly configBase = 'api/konfiguration';

  exportAll(): Observable<Blob> {
    return this.http.get(`${this.configBase}/export`, { responseType: 'blob' });
  }

  importAll(data: any): Observable<any> {
    return this.http.post<any>(`${this.configBase}/import`, data);
  }

  // --- Document Suggestions ---

  private readonly docSugBase = 'api/document-suggestions';

  uploadDocumentSuggestion(file: File, modelConfigId?: string): Observable<DocumentSuggestion> {
    const formData = new FormData();
    formData.append('file', file);
    if (modelConfigId) formData.append('modelConfigId', modelConfigId);
    return this.http.post<DocumentSuggestion>(`${this.docSugBase}/upload`, formData);
  }

  listDocumentSuggestions(): Observable<DocumentSuggestion[]> {
    return this.http.get<DocumentSuggestion[]>(this.docSugBase);
  }

  getDocumentSuggestion(id: number): Observable<DocumentSuggestion> {
    return this.http.get<DocumentSuggestion>(`${this.docSugBase}/${id}`);
  }

  startDocumentSuggestion(id: number): Observable<void> {
    return this.http.post<void>(`${this.docSugBase}/${id}/start`, {});
  }

  deleteDocumentSuggestion(id: number): Observable<void> {
    return this.http.delete<void>(`${this.docSugBase}/${id}`);
  }
}
