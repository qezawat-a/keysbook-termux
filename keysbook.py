#!/usr/bin/env python3
"""KeysBook: fast API key/model browser for Termux.

Keys are encrypted at rest with OpenSSL AES-256-CBC and PBKDF2.
"""
from __future__ import annotations
import base64, getpass, json, os, subprocess, sys
from pathlib import Path
from urllib.parse import urljoin

try:
    import requests
    from rich.console import Console
    from rich.panel import Panel
    from rich.table import Table
    from rich.prompt import Prompt
    from rich.theme import Theme
except ImportError:
    print("Install dependencies first: pip install -r requirements.txt")
    raise

THEME = Theme({
    "title": "bold cyan",
    "accent": "bold bright_cyan",
    "ok": "bold green",
    "bad": "bold red",
    "muted": "dim",
})
console = Console(theme=THEME)

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


def _openssl_env(password: str) -> dict:
    env = os.environ.copy()
    env["KEYSBOOK_VAULT_PASS"] = password
    return env


def openssl_encrypt(text: str, password: str) -> str:
    q = subprocess.run(["openssl", "enc", "-aes-256-cbc", "-pbkdf2", "-salt", "-a", "-pass", "env:KEYSBOOK_VAULT_PASS"], input=text, text=True, capture_output=True, env=_openssl_env(password))
    if q.returncode != 0: raise RuntimeError(q.stderr.strip() or "OpenSSL encryption failed")
    return q.stdout


def openssl_decrypt(encoded: str, password: str) -> str:
    p = subprocess.run(["openssl", "enc", "-d", "-aes-256-cbc", "-pbkdf2", "-a", "-pass", "env:KEYSBOOK_VAULT_PASS"], input=encoded, text=True, capture_output=True, env=_openssl_env(password))
    if p.returncode != 0: raise ValueError("Wrong password or damaged vault")
    return p.stdout


def load_vault() -> list[dict]:
    if not VAULT.exists(): return []
    encoded = VAULT.read_text(encoding="utf-8")
    for attempt in range(3):
        password = getpass.getpass(f"Vault password (attempt {attempt + 1}/3): ")
        try:
            items = json.loads(openssl_decrypt(encoded, password))
            if not isinstance(items, list): raise ValueError("invalid vault data")
            console.print(f"[ok]Loaded {len(items)} saved key(s).[/ok]")
            return items
        except (ValueError, json.JSONDecodeError, OSError):
            console.print("[bad]Wrong Vault password. Saved data was not deleted.[/bad]")
    console.print(f"[bad]Vault could not be opened: {VAULT}[/bad]")
    return []


def save_vault(items: list[dict], password: str | None = None):
    APP_DIR.mkdir(mode=0o700, exist_ok=True)
    if password is None: password = getpass.getpass("Vault password: ")
    if not password: raise ValueError("Vault password cannot be empty")
    temp = VAULT.with_suffix(".tmp")
    temp.write_text(openssl_encrypt(json.dumps(items), password), encoding="utf-8")
    temp.chmod(0o600)
    temp.replace(VAULT)
    console.print(f"[ok]Saved {len(items)} key(s) to {VAULT}[/ok]")


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
    table = Table(title="Available Providers", header_style="bold bright_cyan", border_style="cyan")
    table.add_column("#", justify="right", style="yellow")
    table.add_column("Provider", style="white")
    table.add_column("Base URL", style="green")
    for i, (name, url) in enumerate(PROVIDERS, 1):
        table.add_row(str(i), name, url or "[custom URL]")
    console.print(table)
    while True:
        try: idx = int(Prompt.ask("[accent]Provider number[/accent]")) - 1
        except ValueError: continue
        if 0 <= idx < len(PROVIDERS):
            name, url = PROVIDERS[idx]
            if not url: url = Prompt.ask("[accent]Base URL[/accent]").strip()
            return name, url


def add_key(items):
    name, url = choose_provider()
    api_key = getpass.getpass("API Key / Token (hidden): ").strip()
    label = input("Label (optional): ").strip() or name
    items.append({"provider": name, "label": label, "base_url": url, "api_key": api_key})
    save_vault(items, getpass.getpass("Vault password to save: "))
    print("Saved. Testing models...")
    show_models(items[-1])


def test_model(item: dict, model: str) -> tuple[bool, str]:
    """Send one minimal request. This verifies actual inference access, not just listing access."""
    base = normalize(item["base_url"])
    if item["provider"] == "Anthropic":
        url = urljoin(base, "v1/messages")
        headers = {"x-api-key": item["api_key"], "anthropic-version": "2023-06-01", "content-type": "application/json"}
        body = {"model": model, "max_tokens": 1, "messages": [{"role": "user", "content": "ping"}]}
    else:
        url = urljoin(base, "chat/completions")
        headers = {"Authorization": "Bearer " + item["api_key"], "content-type": "application/json"}
        body = {"model": model, "messages": [{"role": "user", "content": "ping"}], "max_tokens": 1}
    try:
        r = requests.post(url, headers=headers, json=body, timeout=25)
        if r.ok: return True, "Working"
        try: detail = r.json().get("error", {}).get("message", r.reason)
        except Exception: detail = r.reason
        return False, str(detail)[:55]
    except Exception as exc:
        return False, str(exc)[:55]


def show_models(item):
    try:
        models = fetch_models(item)
        if not models:
            console.print("[bad]No models returned for this key.[/bad]"); return
        usable = []
        console.print(f"[muted]Testing {len(models)} models with one minimal request each; failed models will be hidden.[/muted]")
        for number, model in enumerate(models, 1):
            with console.status(f"[accent]Testing {number}/{len(models)}[/accent] {model}"):
                ok, _ = test_model(item, model)
            if ok: usable.append(model)
        if not usable:
            console.print("[bad]No usable models found. Rate-limited, unpaid, unauthorized, or failed models are hidden.[/bad]")
            return
        table = Table(title=f"{item['provider']} — Usable Models", header_style="bold bright_cyan", border_style="cyan")
        table.add_column("#", justify="right", style="yellow")
        table.add_column("Model", style="white", overflow="ellipsis")
        table.add_column("Status", style="green")
        for i, model in enumerate(usable, 1): table.add_row(str(i), model, "Working")
        console.print(table)
        while True:
            choice = Prompt.ask("[accent]Model number to copy | r=refresh | Enter=back[/accent]", default="").strip().lower()
            if not choice: break
            if choice == "r": return show_models(item)
            if choice.isdigit() and 1 <= int(choice) <= len(usable): clipboard(usable[int(choice)-1]); break
    except Exception as e:
        console.print(f"[bad]Could not load models:[/bad] {e}")


def key_table(items):
    table = Table(title="Saved API Keys", header_style="bold bright_cyan", border_style="cyan")
    table.add_column("#", justify="right", style="yellow")
    table.add_column("Label", style="white")
    table.add_column("Provider", style="cyan")
    table.add_column("Base URL", style="green")
    table.add_column("Key", style="yellow")
    for i, x in enumerate(items, 1):
        table.add_row(str(i), x["label"], x["provider"], x["base_url"], x["api_key"][:4] + "…")
    console.print(table)


def main():
    console.print(Panel.fit("[title]KeysBook[/title]\n[muted]Fast API Model Browser for Termux[/muted]", border_style="cyan"))
    items = load_vault()
    while True:
        console.print(Panel("[accent]1[/accent] Add API key    [accent]2[/accent] List keys    [accent]3[/accent] Models\n[accent]4[/accent] Copy key       [accent]5[/accent] Copy URL     [accent]6[/accent] Delete\n[accent]0[/accent] Exit", title="[title]Main Menu[/title]", border_style="cyan"))
        c = Prompt.ask("[accent]Select[/accent]", default="0").strip()
        if c == "1": add_key(items)
        elif c == "2":
            if items: key_table(items)
            else: console.print("[muted]No saved keys.[/muted]")
        elif c == "3":
            if not items: console.print("[muted]No saved keys.[/muted]"); continue
            key_table(items)
            try: show_models(items[int(Prompt.ask("Key number"))-1])
            except (ValueError, IndexError): console.print("[bad]Invalid selection.[/bad]")
        elif c in ("4", "5"):
            if not items: console.print("[muted]No saved keys.[/muted]"); continue
            key_table(items)
            try:
                x = items[int(Prompt.ask("Key number"))-1]; clipboard(x["api_key"] if c == "4" else x["base_url"])
            except (ValueError, IndexError): console.print("[bad]Invalid selection.[/bad]")
        elif c == "6":
            for i, x in enumerate(items, 1): print(f"{i}. {x['label']} [{x['provider']}]")
            try:
                del items[int(Prompt.ask("Delete number"))-1]; save_vault(items, getpass.getpass("Vault password to save: "))
            except (ValueError, IndexError): console.print("[bad]Invalid selection.[/bad]")
        elif c == "0": break

if __name__ == "__main__": main()
