import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-history',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './history.component.html',
  styleUrls: ['./history.component.scss']
})
export class HistoryComponent implements OnInit {
  http = inject(HttpClient);
  
  generations: any[] = [];
  page = 0;
  
  ngOnInit() {
    this.fetchHistory();
  }
  
  fetchHistory() {
    this.http.get<any[]>(`${environment.apiBaseUrl}/generations?page=${this.page}`).subscribe(res => {
      this.generations = res;
    });
  }
  
  getAudioUrl(id: number) {
    return `${environment.apiBaseUrl}/tts/audio/${id}`;
  }
  
  deleteGeneration(id: number) {
    if (!confirm('Delete this generation permanently?')) return;
    this.http.delete(`${environment.apiBaseUrl}/generations/${id}`).subscribe(() => {
      this.generations = this.generations.filter(g => g.id !== id);
    });
  }
  
  loadMore() {
    this.page++;
    this.http.get<any[]>(`${environment.apiBaseUrl}/generations?page=${this.page}`).subscribe(res => {
      this.generations = [...this.generations, ...res];
    });
  }

  toggleLike(generation: any) {
    // Optimistic UI update
    generation.is_liked = !generation.is_liked;
    this.http.post(`${environment.apiBaseUrl}/tts/${generation.id}/like`, {}).subscribe({
      error: () => {
        // Revert on error
        generation.is_liked = !generation.is_liked;
      }
    });
  }
}
