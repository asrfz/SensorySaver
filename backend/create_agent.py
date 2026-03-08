import requests
import json

API_KEY = "sk_4d6fe7a3125b39d2e6fe0f5e669bb55cab98d932c4ba286e"

payload = {
    "name": "Calmify Companion",
    "conversation_config": {
        "agent": {
            "first_message": (
                "Hey, you did so well! I am so proud of you. "
                "Can you tell me what made you feel overwhelmed today?"
            ),
            "language": "en",
            "prompt": {
                "prompt": (
                    "You are Calmify, a warm and gentle AI companion for children "
                    "with autism who just finished a grounding exercise after sensory "
                    "overstimulation.\n\n"
                    "Your job:\n"
                    "1. Ask what triggered their overstimulation "
                    "(loud noises, crowds, textures, routine changes, etc.)\n"
                    "2. Listen carefully to their answer and validate their feelings "
                    "with short supportive words\n"
                    "3. Give one simple coping tip they can try next time\n"
                    "4. Then say: If you want, you can tap the button on screen to "
                    "book an appointment with a therapist in Waterloo who can help "
                    "even more.\n\n"
                    "Rules:\n"
                    "- Keep ALL responses to 1-2 short sentences\n"
                    "- Use simple child-friendly language\n"
                    "- Be warm, patient, and encouraging\n"
                    "- Never use medical jargon\n"
                    "- If they do not want to talk, say that is completely okay "
                    "and you are here whenever they need\n"
                    "- Always mention the Waterloo therapist option at least once"
                ),
                "llm": "gpt-4o-mini",
                "temperature": 0.7,
            },
        }
    },
}

resp = requests.post(
    "https://api.elevenlabs.io/v1/convai/agents/create",
    headers={
        "xi-api-key": API_KEY,
        "Content-Type": "application/json",
    },
    json=payload,
)

print("Status:", resp.status_code)
print("Response:", json.dumps(resp.json(), indent=2))
