export interface Voice {
  id: number;
  engine_voice_id: string;
  display_name: string;
  gender: string;
  style_tag: string;
  description?: string;
}

export interface ProjectSummary {
  id: number;
  name: string;
}

export interface ScriptPreset {
  label: string;
  text: string;
}

export interface SelectOption<T> {
  value: T;
  label: string;
}

export interface QualityPreset {
  steps: number;
  label: 'Draft' | 'Standard' | 'High' | 'Ultra';
  description: string;
}

export type VoiceTab = 'Male' | 'Female';
export type GenerationState = 'idle' | 'processing' | 'ready' | 'failed';
export type StudioLanguage = 'na' | 'hi' | 'en';

export interface GenerateAudioRequest {
  text: string;
  voiceId: string;
  engineVoiceId: string;
  lang: StudioLanguage;
  speed: number;
  totalSteps: number;
  projectId: number | null;
}
