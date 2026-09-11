const BACKEND_URL = "http://localhost:8080/api/skim/process";

const MAX_CONTENT_LENGTH = 8000;
const MIN_CONTENT_LENGTH = 10;

const statusEl = document.getElementById("status");
const selectionInfoEl = document.getElementById("selection-info");
const resultContainer = document.getElementById("result-container");
const resultLabel = document.getElementById("result-label");
const resultText = document.getElementById("result-text");
const btnSummarize = document.getElementById("btn-summarize");
const btnSuggest = document.getElementById("btn-suggest");
const btnExplain = document.getElementById("btn-explain");
const btnRewrite = document.getElementById("btn-rewrite");
const toneSelect = document.getElementById("tone-select");
const btnCopy = document.getElementById("btn-copy");

const ACTION_BUTTONS = [btnSummarize, btnSuggest, btnExplain, btnRewrite];

const OPERATION_LABELS = {
  summarize: "Summary",
  suggest: "Related topics",
  explain: "Simple explanation",
  rewrite: "Rewrite"
};

// Clear any badge notification once the popup is opened
chrome.action.setBadgeText({ text: "" });

btnSummarize.addEventListener("click", () => runOperation("summarize"));
btnSuggest.addEventListener("click", () => runOperation("suggest"));
btnExplain.addEventListener("click", () => runOperation("explain"));
btnRewrite.addEventListener("click", () => runOperation("rewrite", toneSelect.value));

btnCopy.addEventListener("click", () => {
  navigator.clipboard.writeText(resultText.textContent).then(() => {
    btnCopy.textContent = "Copied!";
    setTimeout(() => (btnCopy.textContent = "Copy"), 1200);
  });
});

// On popup open: restore last result AND check the current selection up front,
// so the user sees whether their selection is usable before clicking anything.
chrome.storage.local.get(
    ["skimStatus", "skimOperation", "skimResult", "skimError"],
    (data) => render(data)
);
refreshSelectionInfo();

async function refreshSelectionInfo() {
  let text = "";
  try {
    text = (await getSelectedTextFromActiveTab()) || "";
  } catch (e) {
    // ignore — treated the same as "no selection" below
  }

  const validity = checkSelectionValidity(text);
  setButtonsEnabled(validity.valid);

  if (!text.trim()) {
    selectionInfoEl.textContent = "Select some text on the page, then come back here.";
    selectionInfoEl.classList.remove("warn");
  } else if (!validity.valid) {
    selectionInfoEl.textContent = validity.message;
    selectionInfoEl.classList.add("warn");
  } else {
    selectionInfoEl.textContent = `${text.trim().length} characters selected`;
    selectionInfoEl.classList.remove("warn");
  }
}

function checkSelectionValidity(text) {
  const length = text.trim().length;
  if (length === 0) {
    return { valid: false, message: "No text is selected on the page." };
  }
  if (length < MIN_CONTENT_LENGTH) {
    return { valid: false, message: `Select a bit more text (at least ${MIN_CONTENT_LENGTH} characters).` };
  }
  if (length > MAX_CONTENT_LENGTH) {
    return {
      valid: false,
      message: `Selection too long (${length.toLocaleString()} / ${MAX_CONTENT_LENGTH.toLocaleString()} characters). Please select a shorter passage.`
    };
  }
  return { valid: true, message: "" };
}

function setButtonsEnabled(enabled) {
  ACTION_BUTTONS.forEach((btn) => (btn.disabled = !enabled));
}

async function runOperation(operation, tone) {
  let selectedText;
  try {
    selectedText = await getSelectedTextFromActiveTab();
  } catch (e) {
    showError("Couldn't read the page selection. Try selecting text again.");
    return;
  }

  const validity = checkSelectionValidity(selectedText || "");
  if (!validity.valid) {
    showError(validity.message);
    return;
  }

  showLoading(operation);

  try {
    const response = await fetch(BACKEND_URL, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ content: selectedText.trim(), operation, tone })
    });

    if (!response.ok) {
      const errorBody = await response.json().catch(() => null);
      throw new Error(errorBody?.error || `Server responded with status ${response.status}`);
    }

    const result = await response.text();
    showResult(operation, result);

    chrome.storage.local.set({
      skimStatus: "done",
      skimOperation: operation,
      skimResult: result,
      skimError: null
    });
  } catch (error) {
    showError(error.message || "Something went wrong");
  }
}

function getSelectedTextFromActiveTab() {
  return new Promise((resolve, reject) => {
    chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
      const tab = tabs[0];
      if (!tab || !tab.id) {
        reject(new Error("No active tab"));
        return;
      }
      chrome.scripting.executeScript(
          {
            target: { tabId: tab.id },
            func: () => window.getSelection().toString()
          },
          (results) => {
            if (chrome.runtime.lastError || !results || !results[0]) {
              reject(new Error(chrome.runtime.lastError?.message || "No result"));
              return;
            }
            resolve(results[0].result);
          }
      );
    });
  });
}

function render(data) {
  if (data.skimStatus === "loading") {
    showLoading(data.skimOperation);
  } else if (data.skimStatus === "done" && data.skimResult) {
    showResult(data.skimOperation, data.skimResult);
  } else if (data.skimStatus === "error" && data.skimError) {
    showError(data.skimError);
  }
}

function showLoading(operation) {
  resultContainer.classList.add("hidden");
  statusEl.classList.remove("hidden", "error");
  statusEl.textContent = `Running ${OPERATION_LABELS[operation] || operation}...`;
}

function showResult(operation, text) {
  statusEl.classList.add("hidden");
  resultContainer.classList.remove("hidden");
  resultLabel.textContent = OPERATION_LABELS[operation] || operation;
  resultText.textContent = text;
}

function showError(message) {
  resultContainer.classList.add("hidden");
  statusEl.classList.remove("hidden");
  statusEl.classList.add("error");
  statusEl.textContent = message;
}