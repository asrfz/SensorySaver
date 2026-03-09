# Calmify

A real-time overstimulation detection and calming system for children with autism. Calmify uses contactless camera-based biometrics (pulse and breathing rate) to detect sensory overload and then triggers a physical grounding exercise through an Arduino-powered squeeze toy, guiding the child through AI-driven recovery (with added comfort features)...all without wearables or physical contact.

[![Demo Video](https://img.youtube.com/vi/ecAcwc9Jw2c/maxresdefault.jpg)](https://www.youtube.com/watch?v=ecAcwc9Jw2c&t=310s)

## How It Works

```
Phone Camera (Presage SDK)
        │
        ▼
  Stress Detection ──► Warning Alert ──► Vitals Summary
        │                                      │
        ▼                                      ▼
  Flask Backend ◄──── Arduino Squeeze Toy ◄── Grounding Exercise
        │
        ▼
  Web Frontend: Squeeze Gauge → Soothing Audio → AI Conversation → Breathing Exercise
```

1. **Detection** — The Android app uses the Presage SmartSpectra SDK in continuous mode to monitor pulse and breathing rate via the front-facing camera. An adaptive baseline is collected from the first 8 readings, then a rolling-average threshold engine detects sustained physiological elevation.

2. **Alert** — When overstimulation is detected, the phone plays a warning tone and displays the child's peak vitals (pulse and breathing rate) on a summary screen.

3. **Grounding** — The child is guided to squeeze a stuffed animal embedded with an Arduino Nano 33 BLE Sense. The IMU detects micro-vibrations from grip pressure and sends squeeze events to the backend via a serial bridge.

4. **Recovery** — A web frontend displays a real-time squeeze gauge. Once enough squeezes are registered, the system plays ElevenLabs-generated soothing music and a calming voice message, then launches a conversational AI agent that asks the child what triggered their overstimulation and offers a coping tip.

5. **Breathing Exercise** — The Android app guides the child through 2 inhale/exhale cycles with live-updating vitals, then displays before/after improvement metrics.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Biometric Sensing | [Presage SmartSpectra SDK](https://presagetech.com) — camera-based rPPG for pulse and breathing rate |
| Android App | Kotlin, AndroidX, CameraX, OkHttp |
| Hardware | Arduino Nano 33 BLE Sense (LSM9DS1 IMU) for squeeze detection |
| Backend | Python, Flask |
| Serial Bridge | PySerial — reads Arduino over USB, forwards grip events to Flask |
| AI Voice | ElevenLabs Text-to-Speech (calming message) + Music Generation (soothing audio) |
| AI Conversation | ElevenLabs Conversational AI Agent (GPT-4o-mini) |
| Frontend | Vanilla HTML/CSS/JavaScript with custom animations |

## Project Structure

```
├── android-app/SensAware/          # Android app (Kotlin)
│   └── app/src/main/
│       ├── java/.../sensaware/
│       │   ├── MainActivity.kt          # Landing screen
│       │   ├── MonitoringActivity.kt    # Camera monitoring + stress detection
│       │   ├── AlertActivity.kt         # Vitals summary after detection
│       │   └── BreathingActivity.kt     # Guided breathing exercise
│       └── res/layout/                  # XML layouts
│
├── arduino/grip_detector/
│   └── grip_detector.ino               # IMU-based squeeze detection
│
├── backend/
│   ├── main.py                          # Flask server — session state + API
│   ├── arduino_serial_bridge.py         # Serial listener → HTTP bridge
│   ├── elevenlabs_service.py            # TTS + music generation + caching
│   ├── create_agent.py                  # ElevenLabs conversational agent setup
│   └── frontend.html                    # Web UI — squeeze gauge, audio, AI chat
```

## Setup

### Prerequisites

- Python 3.9+
- Android Studio (for building the Android app)
- Arduino IDE (for flashing the grip detector)
- An Arduino Nano 33 BLE Sense
- ElevenLabs API key
- Presage SmartSpectra API key

### Backend

```bash
cd backend
pip install flask requests pyserial elevenlabs
python main.py
```

### Arduino Bridge

```bash
cd backend
python arduino_serial_bridge.py
```

Update `SERIAL_PORT` in `arduino_serial_bridge.py` to match your Arduino's port.

### Android App

1. Open `android-app/SensAware` in Android Studio
2. Update `backendUrl` in `MonitoringActivity.kt` to your laptop's IP
3. Update `apiKey` with your Presage SmartSpectra API key
4. Build and run on a physical Android device (camera required)

### Arduino

1. Open `arduino/grip_detector/grip_detector.ino` in Arduino IDE
2. Flash to Arduino Nano 33 BLE Sense
3. Place inside a stuffed animal

## Key Design Decisions

- **Contactless sensing** — Children with autism often have tactile sensitivities that make wearables uncomfortable. Camera-based rPPG requires zero physical contact.
- **Adaptive baseline** — The system calibrates to each individual's resting vitals rather than using fixed thresholds, reducing false positives.
- **Physical grounding** — Deep pressure stimulation through squeezing is an evidence-based calming technique for sensory overload.
- **Multi-modal intervention** — Combines proprioceptive input (squeeze), auditory support (music + voice), conversational AI (talk-through), and guided breathing into a single integrated flow.
