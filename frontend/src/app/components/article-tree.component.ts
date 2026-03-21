import { Component, inject, OnInit, EventEmitter, Output, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ArtikelService } from '../services/artikel.service';
import { ArticleTreeNode } from '../models/artikel.model';

@Component({
  selector: 'app-article-tree',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <div class="tree-panel">
      <div class="tree-header">
        <h3>Artikelbaum</h3>
        <button class="btn-icon refresh-btn" (click)="loadTree()" title="Aktualisieren">&#8635;</button>
      </div>

      <div class="tree-search">
        <input type="text" [(ngModel)]="filterText" placeholder="Filtern..." (input)="onFilter()">
      </div>

      <div class="tree-content" *ngIf="tree.length > 0">
        <ng-container *ngFor="let node of filteredTree">
          <ng-container *ngTemplateOutlet="treeNode; context: { node: node, level: 0 }"></ng-container>
        </ng-container>
      </div>

      <div class="tree-empty" *ngIf="tree.length === 0 && !loading">
        <p>Keine Artikel vorhanden.</p>
      </div>

      <div class="tree-loading" *ngIf="loading">Laden...</div>
    </div>

    <ng-template #treeNode let-node="node" let-level="level">
      <div class="tree-item" [style.paddingLeft.px]="12 + level * 16"
           [class.active]="node.id === selectedId"
           [class.has-children]="node.children?.length > 0">

        <button class="expand-btn" *ngIf="node.children?.length > 0"
                (click)="toggleNode(node, $event)">
          {{ node.expanded ? '&#9660;' : '&#9654;' }}
        </button>
        <span class="expand-placeholder" *ngIf="!node.children?.length"></span>

        <a class="tree-link" [routerLink]="'/artikel/' + node.id"
           [class.draft]="node.status === 'DRAFT'"
           [class.archived]="node.status === 'ARCHIVED'"
           (click)="onSelect(node)">
          <span class="tree-title">{{ node.title }}</span>
          <span class="tree-count" *ngIf="node.totalDescendants > 0">{{ node.totalDescendants }}</span>
        </a>
      </div>

      <div class="subtree" *ngIf="node.expanded && node.children?.length > 0"
           [@.disabled]="true">
        <ng-container *ngFor="let child of node.children">
          <ng-container *ngTemplateOutlet="treeNode; context: { node: child, level: level + 1 }"></ng-container>
        </ng-container>
      </div>
    </ng-template>
  `,
  styles: [`
    .tree-panel { display: flex; flex-direction: column; height: 100%; }
    .tree-header { display: flex; align-items: center; justify-content: space-between; padding: 0.75rem; border-bottom: 1px solid #e5e7eb; }
    .tree-header h3 { font-size: 0.8125rem; font-weight: 600; color: #374151; }
    .refresh-btn { font-size: 1rem; }
    .btn-icon { border: none; background: none; cursor: pointer; color: #9ca3af; padding: 0.25rem; line-height: 1; }
    .btn-icon:hover { color: #006EC7; }
    .tree-search { padding: 0.5rem 0.75rem; border-bottom: 1px solid #f3f4f6; }
    .tree-search input { width: 100%; padding: 0.375rem 0.5rem; font-size: 0.75rem; border: 1px solid #e5e7eb; border-radius: 0.375rem; }
    .tree-content { flex: 1; overflow-y: auto; padding: 0.25rem 0; }
    .tree-item { display: flex; align-items: center; gap: 0.125rem; padding: 0.25rem 0.375rem; cursor: pointer; min-height: 28px; }
    .tree-item:hover { background: #f9fafb; }
    .tree-item.active { background: #eff6ff; }
    .expand-btn { border: none; background: none; cursor: pointer; font-size: 0.5rem; color: #9ca3af; padding: 0 0.25rem; width: 18px; flex-shrink: 0; line-height: 1; }
    .expand-btn:hover { color: #374151; }
    .expand-placeholder { width: 18px; flex-shrink: 0; }
    .tree-link { display: flex; align-items: center; gap: 0.375rem; flex: 1; text-decoration: none; color: #374151; font-size: 0.8125rem; overflow: hidden; min-width: 0; }
    .tree-link.draft { color: #92400e; }
    .tree-link.archived { color: #9ca3af; }
    .tree-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; }
    .tree-count { font-size: 0.625rem; color: #9ca3af; background: #f3f4f6; padding: 0 0.375rem; border-radius: 0.5rem; flex-shrink: 0; }
    .tree-empty, .tree-loading { text-align: center; padding: 1.5rem 0.75rem; color: #9ca3af; font-size: 0.8125rem; }
  `]
})
export class ArticleTreeComponent implements OnInit {
  private svc = inject(ArtikelService);

  @Input() selectedId: string | null = null;
  @Output() articleSelected = new EventEmitter<string>();

  tree: ArticleTreeNode[] = [];
  filteredTree: ArticleTreeNode[] = [];
  filterText = '';
  loading = true;

  ngOnInit(): void {
    this.loadTree();
  }

  loadTree(): void {
    this.loading = true;
    this.svc.getTree().subscribe({
      next: tree => {
        this.tree = this.initExpanded(tree);
        this.filteredTree = this.tree;
        this.loading = false;
      },
      error: () => this.loading = false,
    });
  }

  private initExpanded(nodes: ArticleTreeNode[]): ArticleTreeNode[] {
    return nodes.map(n => ({
      ...n,
      expanded: n.depth === 0, // expand root level by default
      children: n.children ? this.initExpanded(n.children) : [],
    }));
  }

  toggleNode(node: ArticleTreeNode, event: Event): void {
    event.stopPropagation();
    node.expanded = !node.expanded;
  }

  onSelect(node: ArticleTreeNode): void {
    this.selectedId = node.id;
    this.articleSelected.emit(node.id);
  }

  onFilter(): void {
    if (!this.filterText.trim()) {
      this.filteredTree = this.tree;
      return;
    }
    this.filteredTree = this.filterNodes(this.tree, this.filterText.toLowerCase());
  }

  private filterNodes(nodes: ArticleTreeNode[], text: string): ArticleTreeNode[] {
    const result: ArticleTreeNode[] = [];
    for (const node of nodes) {
      const childMatches = this.filterNodes(node.children || [], text);
      if (node.title.toLowerCase().includes(text) || childMatches.length > 0) {
        result.push({
          ...node,
          expanded: true,
          children: childMatches.length > 0 ? childMatches : node.children || [],
        });
      }
    }
    return result;
  }
}
