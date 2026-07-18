import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

interface Voice { id: number; engine_voice_id: string; display_name: string; gender: string; style_tag: string; }

@Component({
  selector: 'app-voice-lab',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './voice-lab.component.html',
  styleUrls: ['./voice-lab.component.scss']
})
export class VoiceLabComponent implements OnInit {
  http = inject(HttpClient);
  voices: Voice[] = [];

  ngOnInit() {
    this.http.get<Voice[]>(`${environment.apiBaseUrl}/voices`).subscribe(res => {
      this.voices = res;
    });
  }
}
