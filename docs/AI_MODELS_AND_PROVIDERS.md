# Dettle — Comprehensive AI Models & Provider Configurations

This document serves as the single source of truth for all AI providers and models configured in Dettle, their endpoints, token context boundaries, reasoning/thinking protocols, and role allocations across the autonomous agent fleet.

---

## Architecture Overview

Dettle implements a dual-track AI infrastructure:
- **Track A (Official APIs)**: Direct, high-speed API connections using official API keys stored on-device in `EncryptedSharedPreferences` (AES-256 GCM backed by the Android Keystore). Supports multi-account pooling and round-robin waterfall failover.
- **Track B (WebView Subscriptions)**: Hidden, persistent WebView sessions with automated DOM manipulation enabling use of personal ChatGPT Plus/Pro, Claude Pro, DeepSeek, and Grok subscriptions without per-token API charges.

---

## 1. Provider & Model Specifications

### 1. Google AI Studio (Gemini)
* **Base URL**: `https://generativelanguage.googleapis.com/v1beta`
* **Protocol**: Native Gemini REST + SSE streaming (`/models/{modelId}:streamGenerateContent?key={apiKey}&alt=sse`)
* **Thinking Protocol**: Controlled via `generationConfig.thinkingConfig`
  * `thinkingLevel`: `"minimal" | "low" | "medium" | "high"` (used for Gemini 3.x series)
  * `thinkingBudget`: Integer (e.g. `2048`, `4096`; `0` disables thinking for Gemini 2.5 series)
  * `includeThoughts`: `true` (streams reasoning traces in candidate parts)

#### Models:
| Model ID | Display Name | Context Window | Max Output | Thinking Config | Tier / Best For |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `gemini-3.8-flash` | Gemini 3.8 Flash | 1,048,576 | 65,536 | `thinkingLevel: "medium"` | `HEAVY_CODING` — Primary high-throughput agent loops, coding |
| `gemini-3.5-flash-lite` | Gemini 3.5 Flash-Lite | 1,048,576 | 65,536 | `thinkingLevel: "minimal"` | `FAST_ROUTING` — Fast subagent dispatch, live parsing |
| `gemini-3.1-pro-preview` | Gemini 3.1 Pro | 2,097,152 | 65,536 | `thinkingLevel: "high"` | `DEEP_REASONING` — Architectural planning, multi-file refactors |
| `gemini-3-flash-preview` | Gemini 3 Flash Preview | 1,048,576 | 65,536 | `thinkingLevel: "medium"` | `BALANCED` — Frontier Flash preview |
| `gemini-2.5-flash` | Gemini 2.5 Flash | 1,048,576 | 8,192 | `thinkingBudget: 2048` | `HEAVY_CODING` — Stable fallback |
| `gemini-2.5-pro` | Gemini 2.5 Pro | 2,097,152 | 8,192 | `thinkingBudget: 4096` | `DEEP_REASONING` — Stable 2M context deep analysis |

---

### 2. Groq (LPU Ultra-Fast Inference)
* **Base URL**: `https://api.groq.com/openai/v1`
* **Protocol**: OpenAI-compatible Chat Completions (`/chat/completions`)
* **Speed**: 300 to 1,200+ tokens/second

#### Models:
| Model ID | Display Name | Context Window | Max Output | Tier / Best For |
| :--- | :--- | :--- | :--- | :--- |
| `llama-3.1-8b-instant` | Llama 3.1 8B (Groq) | 131,072 | 131,072 | `FAST_ROUTING` — < 400ms intent classification, reflex actions |
| `llama-3.3-70b-versatile` | Llama 3.3 70B (Groq) | 131,072 | 32,768 | `HEAVY_CODING` — Fast orchestrator decisions, code generation |
| `openai/gpt-oss-120b` | GPT OSS 120B (Groq) | 131,072 | 32,768 | `DEEP_REASONING` — Open-weights frontier reasoning on LPUs |

---

### 3. OpenAI (Direct Official API)
* **Base URL**: `https://api.openai.com/v1`
* **Protocol**: OpenAI Chat Completions (`/chat/completions`)
* **Reasoning Protocol**: `reasoning_effort: "low" | "medium" | "high"` for o1 / o3 models

#### Models:
| Model ID | Display Name | Context Window | Max Output | Thinking Config | Tier / Best For |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `gpt-5` | GPT-5 | 256,000 | 32,768 | Built-in router | `FLAGSHIP` — Complex multimodal agentic synthesis |
| `o3-mini` | o3-mini | 200,000 | 100,000 | `reasoning_effort: "medium"` | `DEEP_REASONING` — Coding, math, unit testing |
| `o1` | o1 | 200,000 | 100,000 | `reasoning_effort: "medium"` | `DEEP_REASONING` — System architecture, hard bugs |
| `gpt-4o` | GPT-4o | 128,000 | 16,384 | None | `BALANCED` — Reliable coding, strict tool following |
| `gpt-4o-mini` | GPT-4o Mini | 128,000 | 16,384 | None | `FAST_ROUTING` — Everyday lightweight tasks |

---

### 4. Anthropic Claude (Direct Official API)
* **Base URL**: `https://api.anthropic.com/v1`
* **Protocol**: Anthropic Messages API (`/messages`)
* **Thinking Protocol**: `thinking: { "type": "enabled", "budget_tokens": 4096 }` (min 1,024; max_tokens > budget_tokens)

#### Models:
| Model ID | Display Name | Context Window | Max Output | Thinking Config | Tier / Best For |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `claude-sonnet-5` | Claude Sonnet 5 | 1,000,000 | 64,000 | Adaptive Thinking | `FLAGSHIP` — Gold-standard agentic coding, refactoring |
| `claude-3-7-sonnet` | Claude 3.7 Sonnet | 200,000 | 64,000 | `budget_tokens: 4096` | `HEAVY_CODING` — Precision code synthesis, PR review |
| `claude-haiku-4.5` | Claude Haiku 4.5 | 200,000 | 8,192 | None | `FAST_ROUTING` — Low latency agent turns, linting |
| `claude-opus-5.5` | Claude Opus 5.5 | 1,000,000 | 64,000 | `budget_tokens: 8192` | `DEEP_REASONING` — Maximum intelligence agent coordination |

---

### 5. DeepSeek (Direct Official API)
* **Base URL**: `https://api.deepseek.com`
* **Protocol**: OpenAI-compatible Chat Completions (`/chat/completions`)
* **Thinking Protocol**: Streams `choices[0].delta.reasoning_content` in SSE chunks

#### Models:
| Model ID | Display Name | Context Window | Max Output | Tier / Best For |
| :--- | :--- | :--- | :--- | :--- |
| `deepseek-v4-pro` | DeepSeek V4 Pro | 128,000 | 8,192 | `HEAVY_CODING` — High-efficiency code generation |
| `deepseek-reasoner` (`deepseek-r1`) | DeepSeek R1 | 128,000 | 8,192 | `DEEP_REASONING` — Open reasoning benchmark leader |
| `deepseek-chat` (`deepseek-v3`) | DeepSeek V3 | 128,000 | 8,192 | `BALANCED` — Fast conversation and utility coding |

---

### 6. Mistral AI (Direct Official API)
* **Base URL**: `https://api.mistral.ai/v1`
* **Protocol**: OpenAI-compatible Chat Completions (`/chat/completions`)

#### Models:
| Model ID | Display Name | Context Window | Max Output | Tier / Best For |
| :--- | :--- | :--- | :--- | :--- |
| `codestral-latest` | Codestral | 256,000 | 16,384 | `HEAVY_CODING` — Specialized code completion, fills, tests |
| `mistral-large-latest` | Mistral Large | 128,000 | 16,384 | `FLAGSHIP` — Complex multilingual reasoning |
| `ministral-8b-latest` | Ministral 8B | 128,000 | 8,192 | `FAST_ROUTING` — Fast edge latency |
| `pixtral-large-latest` | Pixtral Large | 128,000 | 8,192 | `BALANCED` — Multimodal diagrams and screenshot inspection |

---

### 7. OpenRouter (Multi-Provider Free & Paid Aggregator)
* **Base URL**: `https://openrouter.ai/api/v1`
* **Protocol**: OpenAI-compatible Chat Completions (`/chat/completions`)

#### Top Free Models:
| Model ID | Display Name | Context Window | Tier / Best For |
| :--- | :--- | :--- | :--- |
| `meta-llama/llama-3.3-70b-instruct:free` | Llama 3.3 70B (Free) | 131,072 | `HEAVY_CODING` — Rate limit fallback |
| `qwen/qwen-2.5-72b-instruct:free` | Qwen 2.5 72B (Free) | 131,072 | `HEAVY_CODING` — Polyglot coding & math |
| `deepseek/deepseek-r1:free` | DeepSeek R1 (Free) | 64,000 | `DEEP_REASONING` — Free reasoning fallback |
| `nvidia/nemotron-3.5-lightning:free` | Nemotron 3.5 Lightning (Free) | 1,000,000 | `LARGE_CONTEXT` — 1M context reading for free |

---

### 8. SambaNova (Massive Context & Speed)
* **Base URL**: `https://api.sambanova.ai/v1`
* **Protocol**: OpenAI-compatible Chat Completions (`/chat/completions`)

#### Models:
| Model ID | Display Name | Context Window | Tier / Best For |
| :--- | :--- | :--- | :--- |
| `Meta-Llama-3.3-70B-Instruct` | Llama 3.3 70B (SambaNova) | 128,000 | `LARGE_CONTEXT` — High-speed large codebase reading |
| `DeepSeek-R1` | DeepSeek R1 (SambaNova) | 128,000 | `DEEP_REASONING` — High-throughput deep reasoning |
| `Meta-Llama-3.1-405B-Instruct` | Llama 3.1 405B (SambaNova) | 128,000 | `FLAGSHIP` — Largest open weights reasoning |

---

### 9. Cerebras (Wafer-Scale Inference)
* **Base URL**: `https://api.cerebras.ai/v1`
* **Protocol**: OpenAI-compatible Chat Completions (`/chat/completions`)

#### Models:
| Model ID | Display Name | Context Window | Speed | Tier / Best For |
| :--- | :--- | :--- | :--- | :--- |
| `openai/gpt-oss-120b` | GPT OSS 120B (Cerebras) | 131,072 | ~1,500 t/s | `DEEP_REASONING` — Ultra-fast open reasoning |
| `llama-3.3-70b` | Llama 3.3 70B (Cerebras) | 131,072 | ~1,200 t/s | `HEAVY_CODING` — Ultra-fast code generation |

---

### 10. xAI / Grok (Direct Official API)
* **Base URL**: `https://api.x.ai/v1`
* **Protocol**: OpenAI-compatible Chat Completions (`/chat/completions`)

#### Models:
| Model ID | Display Name | Context Window | Tier / Best For |
| :--- | :--- | :--- | :--- |
| `grok-3` | Grok 3 (xAI) | 131,072 | `FLAGSHIP` — Frontier agentic reasoning |
| `grok-3-mini` | Grok 3 Mini (xAI) | 131,072 | `DEEP_REASONING` — Fast reasoning with thinking traces |
| `grok-2` | Grok 2 (xAI) | 128,000 | `BALANCED` — Reliable coding & research |

---

### 11. Local & Network Ollama (Uncensored)
* **Base URL**: `http://10.0.2.2:11434/api` (emulator host) or custom LAN IP
* **Protocol**: Ollama Native / OpenAI-compatible

#### Models:
| Model ID | Display Name | Context Window | Tier / Best For |
| :--- | :--- | :--- | :--- |
| `dolphin-mixtral` | Dolphin Mixtral Uncensored | 32,000 | `UNCENSORED` — Zero refusals, autonomous execution |
| `qwen2.5-coder:32b` | Qwen 2.5 Coder 32B | 32,000 | `HEAVY_CODING` — Offline private code generation |
| `deepseek-r1:14b` | DeepSeek R1 14B Local | 32,000 | `DEEP_REASONING` — Offline private reasoning |

---

### 12. Track B: WebView Subscriptions
* Persistent cookie sessions via Android WebView:
  - `CHATGPT_WEB` (`https://chatgpt.com`): GPT-4o / GPT-5 Free & Plus
  - `CLAUDE_WEB` (`https://claude.ai`): Claude 3.7 / Claude Sonnet 5 Free & Pro
  - `DEEPSEEK_WEB` (`https://chat.deepseek.com`): DeepSeek V3 / R1
  - `GROK_WEB` (`https://grok.com`): Grok 2 / 3 Free & SuperGrok
  - `GEMINI_WEB` (`https://gemini.google.com`): Gemini Advanced
  - `KIMI_WEB` (`https://kimi.ai`): Kimi AI
  - `MISTRAL_WEB` (`https://chat.mistral.ai`): Mistral Chat (Le Chat)
  - `QWEN_WEB` (`https://chat.qwenlm.ai`): Tongyi Qwen Chat

---

## 2. Default Waterfall & Curated Model Sets

### Default Waterfall Order
When executing an agent loop, `KeyPoolManager` attempts providers in this prioritized sequence:
1. `FreeModels.GROQ_LLAMA_8B` — Sub-second routing and fast early decisions
2. `GeminiModels.GEMINI_3_8_FLASH` — Primary 1M context heavy code generation
3. `GeminiModels.GEMINI_3_5_FLASH_LITE` — High-throughput agent turns & document parsing
4. `GeminiModels.GEMINI_3_1_PRO` — Deep reasoning and architectural decomposition
5. `GroqModels.LLAMA_3_3_70B_VERSATILE` — Fast 70B reasoning
6. `FreeModels.SAMBANOVA_LLAMA_70B` — High-speed 128k context reads
7. `FreeModels.OPENROUTER_LLAMA` — General fallback
8. `FreeModels.OPENROUTER_QWEN` — Polyglot fallback
9. `FreeModels.GITHUB_GPT4O` — Quality fallback
10. `FreeModels.GITHUB_DEEPSEEK_R1` — Deep reasoning fallback

### Curated Role Sets
* **`ModelSets.FAST`**: Intent classification, tool reflex checks, inner-loop classifications
  * `FreeModels.GROQ_LLAMA_8B`, `GeminiModels.GEMINI_3_5_FLASH_LITE`, `GeminiModels.GEMINI_3_8_FLASH`, `FreeModels.OPENROUTER_LLAMA`, `FreeModels.GROQ_LLAMA_70B`
* **`ModelSets.LARGE_CONTEXT`**: Entire repository indexing, long documentation reads
  * `GeminiModels.GEMINI_3_8_FLASH` (1M), `GeminiModels.GEMINI_3_1_PRO` (2M), `FreeModels.SAMBANOVA_LLAMA_70B` (128k), `OpenRouterModels.NEMOTRON_3_5_FREE` (1M), `MistralModels.CODESTRAL` (256k)
* **`ModelSets.DEEP_REASONING`**: Architectural planning, complex bug diagnosis, state machine design
  * `GeminiModels.GEMINI_3_1_PRO`, `OpenAIModels.O3_MINI`, `AnthropicModels.CLAUDE_SONNET_5`, `DeepSeekModels.DEEPSEEK_R1`, `FreeModels.GITHUB_DEEPSEEK_R1`, `FreeModels.GROQ_LLAMA_70B`
* **`ModelSets.BEST_QUALITY`**: Critical code synthesis, production releases
  * `AnthropicModels.CLAUDE_SONNET_5`, `OpenAIModels.GPT_5`, `GeminiModels.GEMINI_3_1_PRO`, `MistralModels.CODESTRAL`, `FreeModels.GITHUB_GPT4O`, `GeminiModels.GEMINI_3_8_FLASH`
* **`ModelSets.CODE_REVIEW`**: Coder/Reviewer debate, pull request critiques
  * `GeminiModels.GEMINI_3_1_PRO`, `AnthropicModels.CLAUDE_3_7_SONNET`, `DeepSeekModels.DEEPSEEK_R1`, `GeminiModels.GEMINI_3_8_FLASH`
