# Skim — AI-Powered Text Summarizer (Chrome Extension)

Skim lets you select any text on a webpage and run it through an AI model to summarize it, get related topic suggestions, get a simplified explanation, or rewrite it in a different tone — all without leaving the page.

## Tech Stack

- **Backend:** Java, Spring Boot, Spring AI (`ChatClient`), Gemini API
- **Frontend:** Chrome Extension (Manifest V3) — vanilla JS, HTML, CSS
- **Communication:** REST (JSON over HTTP)

## Project Structure

```
skim-extension/
├── manifest.json        # Extension config, permissions, context menu capability
├── background.js         # Service worker: context menu setup + click handling
├── popup.html             # Popup UI markup
├── popup.js               # Popup logic: manual triggers, selection reading, rendering
├── popup.css              # Popup styling
└── backend/
    ├── SkimRequest.java   # Request DTO (content, operation, tone)
    └── SkimService.java   # Builds AI prompts based on operation/tone

(backend also includes SkimController and AiService, built earlier)
```

## Features (current)

| Operation  | Description                                      | Trigger                          |
|------------|---------------------------------------------------|-----------------------------------|
| Summarize  | Concise summary of selected text                 | Right-click menu / popup button   |
| Suggest    | Related topics & further reading                 | Right-click menu / popup button   |
| Explain    | Simplified, jargon-free explanation               | Right-click menu / popup button   |
| Rewrite    | Rewrite selection in a chosen tone (formal, casual, fix-grammar, shorten, expand) | Right-click submenu / popup dropdown |

## How It Works

1. User selects text on any webpage.
2. Trigger via **right-click → Skim menu** or the **extension popup**.
3. The extension sends `{ content, operation, tone? }` to the backend (`POST /api/skim/process`).
4. `SkimService` builds an operation-specific prompt and calls `AiService`, which forwards it to Gemini via Spring AI's `ChatClient`.
5. The AI response is returned and displayed in the popup (with loading and error states handled on the frontend).

## Backend Setup

1. Clone the backend repo and add your Gemini API key to `application.properties` / environment variables (as required by your Spring AI config).
2. Run the Spring Boot app — it should start on `http://localhost:8080`.
3. Confirm the endpoint is live: `POST http://localhost:8080/api/skim/process` with a JSON body like:
   ```json
   { "content": "some text", "operation": "summarize" }
   ```

## Extension Setup (local, unpacked)

1. Download/clone the `skim-extension` folder (keep all files at the top level).
2. Open `chrome://extensions` in Chrome.
3. Enable **Developer mode** (top-right toggle).
4. Click **Load unpacked** and select the `skim-extension` folder.
5. Pin the Skim icon to your toolbar for easy access.
6. Make sure the backend is running locally on port 8080 before testing.

### Reloading after changes
Chrome doesn't hot-reload extensions. After editing any file, go to `chrome://extensions` and click the reload icon on the Skim card.

### Debugging
- **Background script:** on `chrome://extensions`, click "service worker" under the Skim card to open its console.
- **Popup:** right-click inside the open popup → "Inspect."

## Roadmap

**Done:** context menu integration, popup UI, summarize/suggest/explain/rewrite operations, basic error handling.

**Next up:**
- Keyboard shortcut trigger (`chrome.commands`)
- Rate limiting on the backend (per-IP or per-install)
- Response DTO instead of raw string
- Caching identical requests
- Deployment (hosted backend instead of localhost)
- History/persistence of past results
- Streaming responses
- Page-level (full article) summarization
