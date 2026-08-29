# KeysBook برای Termux

KeysBook یک برنامهٔ سریع Python برای نگهداری API/PAT Key و مشاهدهٔ مدل‌های قابل‌دسترسی Providerها است. نسخهٔ اول عمداً بدون رابط سنگین ساخته شده تا در Termux سریع اجرا شود.

## نصب در Termux

```bash
pkg update -y
pkg install python termux-api -y
pip install -r requirements.txt
python keysbook.py
```

برای کارکردن Copy به کلیپ‌بورد، برنامهٔ **Termux:API** را نیز از منبع سازگار با Termux نصب کن. اگر نصب نباشد، برنامه مقدار را در ترمینال نمایش می‌دهد تا دستی کپی شود.

## روند استفاده

از منوی اصلی گزینهٔ `1` را بزن، Provider را انتخاب کن، API Key را وارد کن و یک رمز برای Vault تعیین کن. کلیدها در `~/.keysbook/vault.json` به‌صورت رمزنگاری‌شده ذخیره می‌شوند. بعد از ذخیره، برنامه به endpoint مدل‌ها درخواست می‌فرستد و فقط مدل‌هایی را نشان می‌دهد که آن حساب و آن کلید واقعاً برمی‌گرداند.

گزینهٔ `Custom Provider` در انتهای فهرست قرار دارد. برای Providerهای ناشناخته یا سرویس‌های شخصی، نام، Base URL و Key را خودت وارد کن. Base URL باید ریشهٔ API باشد؛ برای بیشتر سرویس‌های OpenAI-compatible مانند `https://example.com/v1`، برنامه مسیر `/models` را به آن اضافه می‌کند.

## نکتهٔ امنیتی

API Keyها در صفحه کامل چاپ نمی‌شوند و در فایل Vault با رمز عبور رمزنگاری می‌شوند. رمز Vault را فراموش نکن؛ بازیابی آن از داخل برنامه ممکن نیست. فایل Vault را برای دیگران ارسال نکن و آن را داخل GitHub commit نکن.

## Providerهای آماده

Providerهای موجود شامل OpenAI، Anthropic، xAI، Qwen، Together AI، DeepInfra، SiliconFlow، Nebius، Featherless، Chutes، TokenRouter، NaraRoute، NVIDIA NIM، Azure OpenAI، OpenRouter، Mistral، Codestral، DeepSeek، Kimi، MiniMax، Vercel AI Gateway، Bedrock، Hugging Face، GitHub Models، Z.AI، Fireworks، Gemini، Groq، SambaNova، Cerebras، Ollama و چند مورد دیگر هستند. بعضی Providerها به Base URL اختصاصی حساب، شناسهٔ اکانت، هدر اضافه یا سازگاری متفاوت نیاز دارند و در این نسخه می‌توانند از طریق Custom تنظیم شوند.
