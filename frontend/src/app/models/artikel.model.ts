export interface Grouping {
  id: string;
  name: string;
  description: string | null;
}

export interface Article {
  id: string;
  tenantId: string;
  title: string;
  content: string;
  summary: string;
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
  // Hierarchy
  parentArticleId: string | null;
  sortOrder: number;
  treePath: string | null;
  depth: number;
  children?: Article[];
  breadcrumb?: BreadcrumbItem[];
  childCount: number;
  // Category and Tags
  category: Category | null;
  grouping: Grouping | null;
  tags: Tag[];
  createdBy: string;
  updatedBy: string;
  createdAt: string;
  updatedAt: string;
  version: number;
  publicWithinTenant: boolean;
  linkedTaskId: string | null;
  viewCount: number;
  usageCount: number;
  averageRating: number;
  ratingCount: number;
  lastUsedAt: string | null;
}

export interface BreadcrumbItem {
  id: string;
  title: string;
}

export interface ArticleTreeNode {
  id: string;
  title: string;
  status: string;
  parentArticleId: string | null;
  sortOrder: number;
  depth: number;
  totalDescendants: number;
  children: ArticleTreeNode[];
  expanded?: boolean; // UI state
}

export interface Category {
  id: string;
  name: string;
  description: string;
  parentId: string | null;
  orderIndex: number;
}

export interface Tag {
  id: string;
  name: string;
}

export interface ArticleVersion {
  id: string;
  version: number;
  title: string;
  content: string;
  summary: string;
  changedBy: string;
  changedAt: string;
  changeNote: string;
}

export interface ArticlePage {
  content: Article[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface Statistik {
  gesamt: number;
  veroeffentlicht: number;
  entwuerfe: number;
  kategorien: number;
}

export interface SearchResult {
  id: string;
  title: string;
  summary: string | null;
  snippet: string;
  relevanceScore: number;
  searchType: string;
  breadcrumb: string[];
  status: string;
  parentArticleId: string | null;
  depth: number;
}

export interface StructuredResult {
  title: string;
  summary: string;
  content: string;
}

export interface ChatSession {
  id: string;
  title: string;
  modelConfigId: string | null;
  contextType: string | null;
  contextRefId: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ChatMessage {
  id: string;
  sessionId: string;
  role: 'USER' | 'ASSISTANT' | 'SYSTEM';
  content: string;
  sourceRefs: string | null;
  modelId: string | null;
  tokenCount: number | null;
  createdAt: string;
}

export interface SourceReference {
  articleId: string;
  title: string;
  snippet: string;
  relevance: number;
}

export interface LlmModelConfig {
  id: string;
  name: string;
  provider: string;
  model: string;
  active: boolean;
}

export interface ChatRequest {
  sessionId?: string;
  question: string;
  modelConfigId?: string;
  contextType?: string;
  contextRefId?: string;
}

export interface ChatResponse {
  message: ChatMessage;
  sources: SourceReference[];
  sessionId: string;
  sessionTitle: string;
}
