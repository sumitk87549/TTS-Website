import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map, shareReplay } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { GenerateAudioRequest, ProjectSummary, Voice } from '../models/studio.models';

/**
 * API service for the Studio feature.
 *
 * Path updates (aligned with backend GenerationController refactor):
 *   Audio stream: /api/generations/{id}/audio  (was /api/tts/audio/{id})
 *   Like toggle:  /api/generations/{id}/like   (was /api/tts/{id}/like)
 *
 * Preview fix:
 *   Now sends { text, voiceId, lang } matching TtsPreviewRequest DTO.
 */
@Injectable({ providedIn: 'root' })
export class StudioApiService {
  private http = inject(HttpClient);

  /** Cached voice list — fetched once per app session */
  private voices$?: Observable<Voice[]>;

  getVoices(): Observable<Voice[]> {
    this.voices$ ??= this.http.get<Voice[]>(`${environment.apiBaseUrl}/voices`).pipe(
      map(voices => (voices || []).map(voice => ({
        ...voice,
        // Strip "M1 - " / "F3 - " prefix from display names if present
        display_name: voice.display_name.replace(/^(M|F)\d+\s*-\s*/i, '')
      }))),
      shareReplay({ bufferSize: 1, refCount: true })
    );
    return this.voices$;
  }

  getProjects(): Observable<ProjectSummary[]> {
    return this.http.get<ProjectSummary[]>(`${environment.apiBaseUrl}/projects`);
  }

  /**
   * Preview a voice with a short Hindi greeting.
   * Body matches TtsPreviewRequest DTO: { text, voiceId, lang }.
   */
  previewVoice(engineVoiceId: string, lang: string = 'na'): Observable<Blob> {
    return this.http.post(
      `${environment.apiBaseUrl}/public/tts/preview`,
      { text: 'नमस्ते! मैं आपकी आवाज़ हूँ। कैसे हैं आप?', voiceId: engineVoiceId, lang },
      { responseType: 'blob' }
    );
  }

  /**
   * Generate full audio — returns WAV bytes.
   * Body matches TtsGenerateRequest DTO: { text, voiceId, lang, speed, totalSteps, projectId }.
   */
  generateAudio(request: GenerateAudioRequest): Observable<Blob> {
    return this.http.post(
      `${environment.apiBaseUrl}/tts/generate`,
      {
        text: request.text,
        voiceId: request.voiceId,
        lang: request.lang,
        speed: request.speed,
        totalSteps: request.totalSteps,
        projectId: request.projectId,
      },
      { responseType: 'blob' }
    );
  }

  /**
   * Stream saved audio for a specific generation.
   * Path: /api/generations/{id}/audio
   */
  getGenerationAudio(generationId: number): Observable<Blob> {
    return this.http.get(
      `${environment.apiBaseUrl}/generations/${generationId}/audio`,
      { responseType: 'blob' }
    );
  }

  /**
   * Toggle like on a generation.
   * Path: /api/generations/{id}/like
   */
  likeGeneration(generationId: number): Observable<void> {
    return this.http.post<void>(
      `${environment.apiBaseUrl}/generations/${generationId}/like`,
      {}
    );
  }
}
