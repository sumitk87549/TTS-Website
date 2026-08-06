import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map, shareReplay } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { GenerateAudioRequest, ProjectSummary, Voice } from '../models/studio.models';

@Injectable({ providedIn: 'root' })
export class StudioApiService {
  private http = inject(HttpClient);
  private voices$?: Observable<Voice[]>;

  getVoices(): Observable<Voice[]> {
    this.voices$ ??= this.http.get<Voice[]>(`${environment.apiBaseUrl}/voices`).pipe(
      map(voices => (voices || []).map(voice => ({
        ...voice,
        display_name: voice.display_name.replace(/^(M|F)\d+\s*-\s*/i, '')
      }))),
      shareReplay({ bufferSize: 1, refCount: true })
    );
    return this.voices$;
  }

  getProjects(): Observable<ProjectSummary[]> {
    return this.http.get<ProjectSummary[]>(`${environment.apiBaseUrl}/projects`);
  }

  previewVoice(engineVoiceId: string): Observable<Blob> {
    return this.http.post(`${environment.apiBaseUrl}/public/tts/preview`,
      { text: 'नमस्ते! मैं आपकी आवाज़ हूँ। कैसे हैं आप?', engineVoiceId },
      { responseType: 'blob' }
    );
  }

  generateAudio(request: GenerateAudioRequest): Observable<Blob> {
    return this.http.post(`${environment.apiBaseUrl}/tts/generate`, request, { responseType: 'blob' });
  }
}
