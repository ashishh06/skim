# Skim — AI-Powered Text Summarizer (Chrome Extension)

Skim lets you select any text on a webpage and run it through an AI model to summarize it, get related topic suggestions, get a simplified explanation, or rewrite it in a different tone — all without leaving the page. The backend is hardened with input validation, prompt-injection resistance, rate limiting, caching, and concurrency control so it can be safely shared for demos.

## Tech Stack

- **Backend:** Java, Spring Boot, Spring AI (`ChatClient`), Gemini API
- **Frontend:** Chrome Extension (Manifest V3) — vanilla JS, HTML, CSS
- **Communication:** REST (JSON over HTTP)

## Project Structure

```
├── backend
│   ├── src
│   │   ├── main
│   │   │   ├── java
│   │   │   │   └── com
│   │   │   │       └── skim
│   │   │   │           ├── config
│   │   │   │           │   ├── RateLimitProperties.java
│   │   │   │           │   └── WebConfig.java
│   │   │   │           ├── controller
│   │   │   │           │   ├── GlobalExceptionHandler.java
│   │   │   │           │   └── SkimController.java           
│   │   │   │           ├── dto
│   │   │   │           │   └── SkimRequest.java             
│   │   │   │           ├── rateLimit
│   │   │   │           │   ├── RateLimiterService.java
│   │   │   │           │   ├── RateLimitInterceptor.java
│   │   │   │           │   └── RateLimitResult.java
│   │   │   │           ├── service
│   │   │   │           │   ├── AiService.java                
│   │   │   │           │   ├── ConcurrencyLimiterService.java
│   │   │   │           │   ├── GlobalUsageService.java
│   │   │   │           │   ├── ResponseCacheService.java
│   │   │   │           │   └── SkimService.java           
│   │   │   │           ├── util
│   │   │   │           │   ├── AiServiceException.java
│   │   │   │           │   ├── DailyLimitReachedException.java
│   │   │   │           │   └── ServerBusyException.java
│   │   │   │           └── SkimApplication.java
│   │   │   └── resources
│   │   │       ├── static
│   │   │       ├── templates
│   │   │       └── application.properties
├── skim-extension
│   ├── background.js
│   ├── manifest.json
│   ├── popup.css
│   ├── popup.html
│   └── popup.js
```
## Features

**Core operations**
- Summarize — concise summary of selected text
- Suggest — related topics and further reading
- Explain — simplified, jargon-free explanation
- Rewrite — formal, casual, fix-grammar, shorten, or expand, via a tone parameter

**Frontend**
- Right-click context menu for every operation (with a Rewrite submenu for tones)
- Popup UI with manual trigger buttons and a tone dropdown
- Live selection validation — shows character count, warns and disables buttons if the selection is too short/long, before any request is sent
- Copy-to-clipboard for results
- Last result persists across popup reopens

**Backend robustness**
- Input validation (10–8000 character range, required fields) via Bean Validation
- Content sanitization (strips control characters, normalizes whitespace)
- Prompt-injection resistance — selected text is wrapped in explicit delimiters with an instruction to treat it as data, not commands
- Centralized exception handling — consistent JSON error responses, no leaked stack traces
- Per-IP rate limiting — burst (per minute) and daily caps per client
- Global daily usage cap — protects the shared Gemini free-tier quota regardless of per-IP limits
- Response caching — identical `(content, operation, tone)` requests are served instantly without calling Gemini again
- Concurrency limiting — caps simultaneous in-flight Gemini calls, queuing brief bursts

## How It Works

1. User selects text on any webpage.
2. Trigger via **right-click → Skim menu** or the **extension popup**.
3. Extension validates selection length client-side, then sends `{ content, operation, tone? }` to `POST /api/skim/process`.
4. Request passes through the rate limit interceptor (per-IP check), then bean validation on the DTO.
5. `SkimService` builds an operation-specific, injection-resistant prompt, checks the response cache, then (on a cache miss) checks the global daily quota and acquires a concurrency slot before calling `AiService` → Gemini via Spring AI.
6. Result is cached and returned; the extension displays it with loading/error states handled throughout.

## Backend Setup

1. Add dependencies: `spring-boot-starter-web`, `spring-boot-starter-validation`, Spring AI's Gemini starter.
2. Add your Gemini API key to `application.properties` / environment variables as required by your Spring AI config.
3. Add the rate-limit thresholds from `application-ratelimit.properties.snippet` into your `application.properties` (tune as needed).
4. Run the app — it starts on `http://localhost:8080`.
5. Test the endpoint:
   ```json
   POST http://localhost:8080/api/skim/process
   { "content": "some text longer than ten characters", "operation": "summarize" }
   ```

## Extension Setup (local, unpacked)

1. Download/clone the `skim-extension` folder (keep all files at the top level).
2. Open `chrome://extensions`, enable **Developer mode**.
3. Click **Load unpacked**, select the `skim-extension` folder.
4. Pin the Skim icon to your toolbar.
5. Make sure the backend is running locally on port 8080.

### Reloading after changes
Reload the extension from `chrome://extensions` after editing any file — nothing hot-reloads.

### Debugging
- **Background script:** click "service worker" under the Skim card on `chrome://extensions`.
- **Popup:** right-click inside the open popup → "Inspect."

## Roadmap

**Done:** context menu, popup UI, 4 operations with tone support, input validation, prompt-injection resistance, per-IP + global rate limiting, response caching, concurrency limiting, centralized error handling.

**Next up:**
- Keyboard shortcut trigger (`chrome.commands`)
- Deployment (hosted backend instead of localhost)
- History/persistence of past results
- Streaming responses
- Page-level (full article) summarization
