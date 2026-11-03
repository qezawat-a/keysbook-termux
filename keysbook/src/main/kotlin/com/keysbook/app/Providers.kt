package com.keysbook.app

/**
 * Provider catalogue, ported 1:1 from the Termux Python app (keysbook.py).
 * A blank base URL means the provider needs a user-supplied endpoint.
 */
object Providers {

    data class Provider(val name: String, val baseUrl: String)

    val ALL: List<Provider> = listOf(
        Provider("OpenAI", "https://api.openai.com/v1"),
        Provider("Anthropic", "https://api.anthropic.com"),
        Provider("ClinePass", ""),
        Provider("xAI (Grok)", "https://api.x.ai/v1"),
        Provider("QwenCloud Token Plan", "https://dashscope.aliyuncs.com/compatible-mode/v1"),
        Provider("QwenCloud Coding Plan", "https://coding.dashscope.aliyuncs.com/v1"),
        Provider("Together AI", "https://api.together.xyz/v1"),
        Provider("DeepInfra", "https://api.deepinfra.com/v1/openai"),
        Provider("SiliconFlow", "https://api.siliconflow.com/v1"),
        Provider("Nebius Token Factory", "https://api.tokenfactory.nebius.com/v1"),
        Provider("Agnes", ""),
        Provider("Featherless", "https://api.featherless.ai/v1"),
        Provider("Chutes", "https://llm.chutes.ai/v1"),
        Provider("W&B Inference", "https://api.inference.wandb.ai/v1"),
        Provider("ZenMux", "https://zenmux.ai/api/v1"),
        Provider("Kiro", ""),
        Provider("Kilo", "https://api.kilo.ai/api/gateway"),
        Provider("TokenRouter", "https://api.tokenrouter.com/v1"),
        Provider("NaraRoute", "https://router.bynara.id/v1"),
        Provider("NVIDIA NIM", "https://integrate.api.nvidia.com/v1"),
        Provider("Azure OpenAI", ""),
        Provider("OpenRouter", "https://openrouter.ai/api/v1"),
        Provider("Mistral La Plateforme", "https://api.mistral.ai/v1"),
        Provider("Mistral Codestral", "https://codestral.mistral.ai/v1"),
        Provider("DeepSeek", "https://api.deepseek.com"),
        Provider("Kimi", "https://api.moonshot.ai/v1"),
        Provider("Kimi Code", "https://api.moonshot.ai/v1"),
        Provider("Wafer", ""),
        Provider("MiniMax", "https://api.minimax.io/v1"),
        Provider("OpenCode Zen", "https://opencode.ai/zen/v1"),
        Provider("Vercel AI Gateway", "https://ai-gateway.vercel.sh/v1"),
        Provider("AWS Bedrock", "https://bedrock-mantle.us-east-1.api.aws/v1"),
        Provider("Hugging Face", "https://router.huggingface.co/v1"),
        Provider("Cohere", ""),
        Provider("GitHub Models", "https://models.inference.ai.azure.com"),
        Provider("Z.AI", "https://api.z.ai/api/paas/v4"),
        Provider("Fireworks AI", "https://api.fireworks.ai/inference/v1"),
        Provider("Cloudflare Workers AI", "https://api.cloudflare.com/client/v1"),
        Provider("Gemini", "https://generativelanguage.googleapis.com/v1"),
        Provider("Groq", "https://api.groq.com/openai/v1"),
        Provider("SambaNova", "https://api.sambanova.ai/v1"),
        Provider("Cerebras", "https://api.cerebras.ai/v1"),
        Provider("Ollama Local", "http://127.0.0.1:11434/api"),
        Provider("Ollama Cloud", "https://ollama.com/api"),
        Provider("Custom Provider", "")
    )

    /** Providers whose model listing lives on the Ollama /api/tags shape. */
    fun isOllama(provider: String): Boolean =
        provider == "Ollama Local" || provider == "Ollama Cloud"

    /** Anthropic needs its own auth header and a different models path. */
    fun isAnthropic(provider: String): Boolean = provider == "Anthropic"

    fun defaultBaseUrl(provider: String): String =
        ALL.firstOrNull { it.name == provider }?.baseUrl ?: ""
}
