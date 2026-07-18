import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

interface Project { id: number; name: string; created_at: string; }

@Component({
  selector: 'app-projects',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './projects.component.html',
  styleUrls: ['./projects.component.scss']
})
export class ProjectsComponent implements OnInit {
  http = inject(HttpClient);
  
  projects: Project[] = [];
  generations: any[] = [];
  selectedProject: Project | null = null;
  
  newProjectName = '';
  editingProject: Project | null = null;
  editName = '';

  ngOnInit() {
    this.fetchProjects();
  }

  fetchProjects() {
    this.http.get<Project[]>(`${environment.apiBaseUrl}/projects`).subscribe(res => {
      this.projects = res;
    });
  }

  createProject() {
    if (!this.newProjectName.trim()) return;
    this.http.post(`${environment.apiBaseUrl}/projects`, { name: this.newProjectName }).subscribe(() => {
      this.newProjectName = '';
      this.fetchProjects();
    });
  }

  startEdit(p: Project) {
    this.editingProject = p;
    this.editName = p.name;
  }

  saveEdit() {
    if (!this.editingProject || !this.editName.trim()) return;
    this.http.patch(`${environment.apiBaseUrl}/projects/${this.editingProject.id}`, { name: this.editName }).subscribe(() => {
      this.editingProject = null;
      this.fetchProjects();
    });
  }
  
  cancelEdit() {
    this.editingProject = null;
  }

  deleteProject(id: number) {
    if (!confirm('Delete this project?')) return;
    this.http.delete(`${environment.apiBaseUrl}/projects/${id}`).subscribe(() => {
      if (this.selectedProject?.id === id) this.selectedProject = null;
      this.fetchProjects();
    });
  }

  selectProject(p: Project) {
    this.selectedProject = p;
    this.http.get<any[]>(`${environment.apiBaseUrl}/generations?projectId=${p.id}`).subscribe(res => {
      this.generations = res;
    });
  }

  backToList() {
    this.selectedProject = null;
  }
  
  getAudioUrl(id: number) {
    return `${environment.apiBaseUrl}/tts/audio/${id}`;
  }
  
  deleteGeneration(id: number) {
    if (!confirm('Delete this generation?')) return;
    this.http.delete(`${environment.apiBaseUrl}/generations/${id}`).subscribe(() => {
      this.generations = this.generations.filter(g => g.id !== id);
    });
  }

  toggleLike(generation: any) {
    generation.is_liked = !generation.is_liked;
    this.http.post(`${environment.apiBaseUrl}/tts/${generation.id}/like`, {}).subscribe({
      error: () => {
        generation.is_liked = !generation.is_liked;
      }
    });
  }
}
