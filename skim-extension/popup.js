const BACKEND_URL = "http://localhost:8080/api/skim/process";

const statusEl = document.getElementById("status");
const resultContainer = document.getElementById("result-container");
const resultLabel = document.getElementById("result-label");
const resultText = document.getElementById("result-text");
const btnSummarize = document.getElementById("btn-summarize");
const btnSuggest = document.getElementById("btn-suggest");
const btnCopy = document.getElementById("btn-copy");

const OPERATION_LABELS = {
  summarize: "Summary",
  suggest: "Related topics"
};

// Clear any badge notification once the popup is opened
chrome.action.setBadgeText({ text: "" });

btnSummarize.addEventListener("click", () => runOperation("summarize"));
btnSuggest.addEventListener("click", () => runOperation("suggest"));

btnCopy.addEventListener("click", () => {
  navigator.clipboard.writeText(resultText.textContent).then(() => {
    btnCopy.textContent = "Copied!";
    setTimeout(() => (btnCopy.textContent = "Copy"), 1200);
  });
});

// On popup open, show whatever the last stored result was (e.g. from a context-menu action)
chrome.storage.local.get(
  ["skimStatus", "skimOperation", "skimResult", "skimError"],
  (data) => render(data)
);

async function runOperation(operation) {
  showLoading(operation);

  let selectedText;
  try {
    selectedText = await getSelectedTextFromActiveTab();
  } catch (e) {
    showError("Couldn't read the page selection. Try selecting text again.");
    return;
  }

  if (!selectedText || !selectedText.trim()) {
    showError("No text is selected on the page.");
    return;
  }

  try {
    const response = await fetch(BACKEND_URL, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ content: selectedText, operation })
    });

    if (!response.ok) {
      throw new Error(`Server responded with status ${response.status}`);
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
