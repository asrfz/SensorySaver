import requests
import os
import time
import threading

ELEVENLABS_API_KEY = "sk_4d6fe7a3125b39d2e6fe0f5e669bb55cab98d932c4ba286e"
VOICE_ID = "21m00Tcm4TlvDq8ikWAM"  # "Rachel" — calm, warm voice

AUDIO_DIR = os.path.join(os.path.dirname(__file__), "audio_cache")
os.makedirs(AUDIO_DIR, exist_ok=True)

CALMING_MESSAGE = (
    "Great job! You did really well. "
    "Take a nice, slow deep breath in... and breathe out. "
    "You're safe. Everything is okay. You're doing amazing."
)

MUSIC_PROMPT = (
    "Soft gentle calming piano melody with light ambient pads, "
    "peaceful and soothing, slow tempo, meditation and relaxation music"
)


def _tts(text, filepath):
    if os.path.exists(filepath):
        print(f"Using cached TTS: {filepath}")
        return filepath

    print("Generating calming message via ElevenLabs TTS...")
    url = f"https://api.elevenlabs.io/v1/text-to-speech/{VOICE_ID}"
    headers = {
        "xi-api-key": ELEVENLABS_API_KEY,
        "Content-Type": "application/json",
    }
    payload = {
        "text": text,
        "model_id": "eleven_monolingual_v1",
        "voice_settings": {
            "stability": 0.85,
            "similarity_boost": 0.7,
        },
    }

    resp = requests.post(url, json=payload, headers=headers)
    if resp.status_code == 200:
        with open(filepath, "wb") as f:
            f.write(resp.content)
        print("TTS generated successfully")
        return filepath

    print(f"TTS error: {resp.status_code} {resp.text}")
    return None


def _music(prompt, duration_ms, filepath):
    if os.path.exists(filepath):
        print(f"Using cached music: {filepath}")
        return filepath

    print("Generating soothing music via ElevenLabs Music API...")
    url = "https://api.elevenlabs.io/v1/music"
    headers = {
        "xi-api-key": ELEVENLABS_API_KEY,
        "Content-Type": "application/json",
    }
    payload = {
        "prompt": prompt,
        "music_length_ms": duration_ms,
        "model_id": "music_v1",
        "force_instrumental": True,
    }

    resp = requests.post(url, json=payload, headers=headers, timeout=120)
    if resp.status_code == 200:
        with open(filepath, "wb") as f:
            f.write(resp.content)
        print("Music generated successfully")
        return filepath

    print(f"Music error: {resp.status_code} {resp.text}")
    return None


def _play(filepath):
    try:
        import pygame
        if not pygame.mixer.get_init():
            pygame.mixer.init()
        pygame.mixer.music.load(filepath)
        pygame.mixer.music.play()
        while pygame.mixer.music.get_busy():
            time.sleep(0.1)
    except Exception as e:
        print(f"Playback error: {e}")


def run_calming_sequence():
    """Called after grounding completes. Plays soothing music then calming voice."""
    print("Starting calming audio sequence...")

    music_path = os.path.join(AUDIO_DIR, "soothing_music.mp3")
    message_path = os.path.join(AUDIO_DIR, "calming_message.mp3")

    _music(MUSIC_PROMPT, 5000, music_path)
    _tts(CALMING_MESSAGE, message_path)

    if message_path and os.path.exists(message_path):
        print("Playing calming message...")
        _play(message_path)

    if music_path and os.path.exists(music_path):
        print("Playing soothing music...")
        _play(music_path)

    print("Calming sequence complete.")


def precache_audio():
    """Pre-generate audio at startup so there's no delay after grounding."""
    def _cache():
        _tts(CALMING_MESSAGE, os.path.join(AUDIO_DIR, "calming_message.mp3"))
        _music(MUSIC_PROMPT, 5000, os.path.join(AUDIO_DIR, "soothing_music.mp3"))
        print("Audio pre-cached and ready")

    threading.Thread(target=_cache, daemon=True).start()
