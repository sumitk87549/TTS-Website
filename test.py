import torchaudio as ta
from chatterbox.tts import ChatterboxTTS

model = ChatterboxTTS.from_pretrained(device="cpu")

text = "Don't disturb me!!! I am practising..."
wav = model.generate(text)
ta.save("test-1.wav", wav, model.sr)