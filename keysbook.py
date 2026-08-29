#!/usr/bin/env python3
"""KeysBook: fast API key/model browser for Termux.

Keys are encrypted at rest with a password-derived Fernet key.
"""
from __future__ import annotations
import base64, getpass, hashlib, json, os, secrets, subprocess, sys
from pathlib import Path
from urllib.parse import urljoin

try:
    import requests
except ImportError:
    print("Install dependency first: pip install requests cryptography")
    raise
try:
    from cryptography.fernet import Fernet, InvalidToken
except ImportError:
    print("Install dependency first: pip install requests cryptography")
    raise

APP_DIR = Path.home() / ".keysbook"
VAULT = APP_DIR / "vault.json"

# OpenAI-compatible defaults. Providers marked custom_endpoint need extra fields.
PROVIDERS = [
    ("OpenAI", "https://api.openai.com/v1"),
    ("Anthropic", "https://api.anthropic.com"),
    ("ClinePass", ""), ("xAI (Grok)", "https://api.x.ai/v1"),
    ("QwenCloud Token Plan", "https://dashscope.aliyuncs.com/compatible-mode/v1"),
    ("QwenCloud Coding Plan", "https://coding.dashscope.aliyuncs.com/v1"),
    ("Together AI", "https://api.together.xyz/v1"),
    ("DeepInfra", "https://api.deepinfra.com/v1/openai"),
    ("SiliconFlow", "https://api.siliconflow.com/v1"),
    ("Nebius Token Factory", "https://api.tokenfactory.nebius.com/v1"),
    ("Agnes", ""), ("Featherless", "https://api.featherless.ai/v1"),
    ("Chutes", "https://llm.chutes.ai/v1"), ("W&B Inference", "https://api.inference.wandb.ai/v1"),
    ("ZenMux", "https://zenmux.ai/api/v1"), ("Kiro", ""),
    ("TokenRouter", "https://api.tokenrouter.com/v1"),
    ("NaraRoute", "https://router.bynara.id/v1"),
    ("NVIDIA NIM", "https://integrate.api.nvidia.com/v1"),
    ("Azure OpenAI", ""), ("OpenRouter", "https://openrouter.ai/api/v1"),
    ("Mistral La Plateforme", "https://api.mistral.ai/v1"),
    ("Mistral Codestral", "https://codestral.mistral.ai/v1"),
    ("DeepSeek", "https://api.deepseek.com"), ("Kimi", "https://api.moonshot.ai/v1"),
    ("Kimi Code", "https://api.moonshot.ai/v1"), ("Wafer", ""),
    ("MiniMax", "https://api.minimax.io/v1"), ("OpenCode Zen", "https://opencode.ai/zen/v1"),
    ("Vercel AI Gateway", "https://ai-gateway.vercel.sh/v1"),
    ("AWS Bedrock", "https://bedrock-mantle.us-east-1.api.aws/v1"),
    ("Hugging Face", "https://router.huggingface.co/v1"), ("Cohere", ""),
    ("GitHub Models", "https://models.inference.ai.azure.com"),
    ("Z.AI", "https://api.z.ai/api/paas/v4"), ("Fireworks AI", "https://api.fireworks.ai/inference/v1"),
    ("Cloudflare Workers AI", ""),
    ("Gemini", "https://generativelanguage.googleapis.com/v1beta/openai"),
    ("Groq", "https://api.groq.com/openai/v1"), ("SambaNova", "https://api.sambanova.ai/v1"),
    ("Kilo", ""), ("Cerebras", "https://api.cerebras.ai/v1"),
    ("Ollama Local", "http://127.0.0.1:11434/v1"),
    ("Custom Provider", ""),
]


def key_from_password(password: str, salt: bytes) -> bytes:
    return base64.urlsafe_b64encode(hashlib.pbkdf2_hmac("sha256", password.encode(), salt, 390000, 32))


def load_vault() -> list[dict]:
    if not VAULT.exists(): return []
    password = getpass.getpass("Vault password: ")
    try:
        raw = json.loads(VAULT.read_text())
        f = Fernet(key_from_password(password, base64.b64decode(raw["salt"])))
        return json.loads(f.decrypt(raw["data"].encode()).decode())
    except (InvalidToken, KeyError, ValueError, json.JSONDecodeError):
        print("Wrong password or damaged vault."); return []


def save_vault(items: list[dict], password: str | None = None):
    APP_DIR.mkdir(mode=0o700, exist_ok=True)
    if password is None: password = getpass.getpass("Create vault password: ")
    salt = secrets.token_bytes(16)
    f = Fernet(key_from_password(password, salt))
    payload = {"salt": base64.b64encode(salt).decode(), "data": f.encrypt(json.dumps(items).encode()).decode()}
    VAULT.write_text(json.dumps(payload)); VAULT.chmod(0o600)


def clipboard(text: str):
    try:
        p = subprocess.run(["termux-clipboard-set"], input=text, text=True, capture_output=True)
        if p.returncode == 0: print("Copied to Android clipboard."); return
    except FileNotFoundError: pass
    print("Clipboard unavailable. Install Termux:API or copy this value manually:")
    print(text)


def normalize(url: str) -> str:
    return url.rstrip("/") + "/"


def fetch_models(item: dict) -> list[str]:
    base = normalize(item["base_url"])
    if item["provider"] == "Anthropic":
        headers = {"x-api-key": item["api_key"], "anthropic-version": "2023-06-01", "Accept": "application/json"}
    else:
        headers = {"Authorization": "Bearer " + item["api_key"], "Accept": "application/json"}
        if item["provider"] == "OpenRouter": headers.update({"HTTP-Referer": "https://keysbook.local", "X-Title": "KeysBook"})
    r = requests.get(urljoin(base, "v1/models" if item["provider"] == "Anthropic" else "models"), headers=headers, timeout=20)
    r.raise_for_status()
    data = r.json()
    rows = data.get("data", data if isinstance(data, list) else [])
    return sorted([str(x.get("id", x.get("name", ""))) for x in rows if isinstance(x, dict) and x.get("id", x.get("name"))])


def choose_provider() -> tuple[str, str]:
    for i, (name, url) in enumerate(PROVIDERS, 1): print(f"{i:2}. {name:<26} {url or '[enter custom URL]'}")
    while True:
        try: idx = int(input("Provider number: ")) - 1
        except ValueError: continue
        if 0 <= idx < len(PROVIDERS):
            name, url = PROVIDERS[idx]
            if not url: url = input("Base URL: ").strip()
            return name, url


def add_key(items):
    name, url = choose_provider()
    api_key = getpass.getpass("API Key / Token (hidden): ").strip()
    label = input("Label (optional): ").strip() or name
    items.append({"provider": name, "label": label, "base_url": url, "api_key": api_key})
    save_vault(items, getpass.getpass("Vault password to save: "))
    print("Saved. Testing models...")
    show_models(items[-1])


def show_models(item):
    try:
        models = fetch_models(item)
        print(f"\n{item['provider']} — {len(models)} accessible model(s):")
        for i, model in enumerate(models, 1): print(f"{i:3}. {model}")
        if models:
            while True:
                choice = input("Model number to copy (Enter to return): ").strip()
                if not choice: break
                if choice.isdigit() and 1 <= int(choice) <= len(models): clipboard(models[int(choice)-1]); break
    except Exception as e:
        print(f"Could not load models: {e}")


def main():
    print("\nKeysBook — fast API keys and models for Termux\n")
    items = load_vault()
    while True:
        print("\n1) Add API key   2) List saved keys   3) Test/show models   4) Copy key   5) Copy Base URL   6) Delete   0) Exit")
        c = input("Select: ").strip()
        if c == "1": add_key(items); items = load_vault()
        elif c == "2":
            for i, x in enumerate(items, 1): print(f"{i}. {x['label']} [{x['provider']}] — {x['base_url']} — {x['api_key'][:4]}…")
        elif c == "3":
            if not items: print("No saved keys."); continue
            for i, x in enumerate(items, 1): print(f"{i}. {x['label']} [{x['provider']}]")
            try: show_models(items[int(input("Key number: "))-1])
            except (ValueError, IndexError): print("Invalid selection.")
        elif c in ("4", "5"):
            if not items: print("No saved keys."); continue
            for i, x in enumerate(items, 1): print(f"{i}. {x['label']} [{x['provider']}]")
            try:
                x = items[int(input("Key number: "))-1]; clipboard(x["api_key"] if c == "4" else x["base_url"])
            except (ValueError, IndexError): print("Invalid selection.")
        elif c == "6":
            for i, x in enumerate(items, 1): print(f"{i}. {x['label']} [{x['provider']}]")
            try:
                del items[int(input("Delete number: "))-1]; save_vault(items, getpass.getpass("Vault password to save: "))
            except (ValueError, IndexError): print("Invalid selection.")
        elif c == "0": break

if __name__ == "__main__": main()
