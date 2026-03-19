import requests
import os
import json

key = os.environ.get("GEMINI_API_KEY")
if not key:
    print("No key set in environment. Set GEMINI_API_KEY")
    exit(1)

url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key={key}"

data = {
    "contents": [{"role": "user", "parts": [{"text": "Hello"}]}]
}

resp = requests.post(url, json=data, headers={"Content-Type": "application/json"})
print(resp.status_code)
print(resp.text)
