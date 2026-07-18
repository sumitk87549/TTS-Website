import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

interface Voice { id: number; engine_voice_id: string; display_name: string; gender: string; style_tag: string; }

@Component({
  selector: 'app-studio',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './studio.component.html',
  styleUrls: ['./studio.component.scss']
})
export class StudioComponent implements OnInit {
  http = inject(HttpClient);
  
  text = '';
  maxChars = 1000;
  usage = { charactersUsed: 0, charactersLimit: 5000 };
  
  voices: Voice[] = [];
  maleVoices: Voice[] = [];
  femaleVoices: Voice[] = [];
  
  selectedTab: 'Male' | 'Female' | 'My Voices' = 'Male';
  selectedVoiceId: number | null = null;
  selectedEngineVoiceId: string | null = null;
  
  generating = false;
  error = '';
  audioUrl: string | null = null;
  
  projects: any[] = [];
  selectedProjectId: number | null = null;
  
  ngOnInit() {
    this.fetchUsage();
    this.fetchVoices();
    this.fetchProjects();
  }
  
  fetchUsage() {
    this.http.get<any>(`${environment.apiBaseUrl}/usage/today`).subscribe(res => this.usage = res);
  }
  
  fetchVoices() {
    this.http.get<Voice[]>(`${environment.apiBaseUrl}/voices`).subscribe(res => {
      this.voices = res;
      this.maleVoices = res.filter(v => v.gender === 'male');
      this.femaleVoices = res.filter(v => v.gender === 'female');
      if (this.maleVoices.length > 0) this.selectVoice(this.maleVoices[0]);
    });
  }
  
  fetchProjects() {
    this.http.get<any[]>(`${environment.apiBaseUrl}/projects`).subscribe(res => this.projects = res);
  }
  
  selectVoice(v: Voice) {
    this.selectedVoiceId = v.id;
    this.selectedEngineVoiceId = v.engine_voice_id;
  }
  
  applyStyle(style: string) {
    if (style === 'Story') this.text = 'एक समय की बात है, एक छोटे से गाँव में...';
    if (style === 'Promotional') this.text = 'क्या आप अपने व्यापार को बढ़ाना चाहते हैं? आज ही हमसे जुड़ें!';
    if (style === 'Greeting') this.text = 'नमस्ते! आपका दिन शुभ हो।';
    if (style === 'News') this.text = 'आज की ताज़ा ख़बर: देश भर में मौसम का मिज़ाज बदल रहा है...';
  }
  
  get charsLeft() {
    return this.usage.charactersLimit - this.usage.charactersUsed;
  }
  
  generate() {
    if (!this.text || !this.selectedEngineVoiceId) return;
    this.generating = true;
    this.error = '';
    this.audioUrl = null;
    
    this.http.post<any>(`${environment.apiBaseUrl}/tts/generate`, {
      text: this.text,
      engineVoiceId: this.selectedEngineVoiceId,
      projectId: this.selectedProjectId
    }).subscribe({
      next: (res) => {
        this.audioUrl = `${environment.apiBaseUrl}/tts/audio/${res.id}`;
        this.generating = false;
        this.fetchUsage();
      },
      error: (err) => {
        this.error = err.error?.error || 'Failed to generate audio';
        this.generating = false;
      }
    });
  }
}
