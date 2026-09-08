// ---- Config ----
const BACKEND_URL = "http://localhost:8080/api/skim/process";

// ---- Context menu setup ----
chrome.runtime.onInstalled.addListener(() => {
  chrome.contextMenus.create({
    id: "skim-summarize",
    title: "Skim: Summarize",
    contexts: ["selection"]
  });
  chrome.contextMenus.create({
    id: "skim-suggest",
    title: "Skim: Suggest related topics",
    contexts: ["selection"]
  });
});

// ---- Handle context menu clicks ----
chrome.contextMenus.onClicked.addListener(async (info) => {
  const operation = info.menuItemId === "skim-summarize" ? "summarize" : "suggest";
  const content = info.selectionText;
  if (!content) return;

  await chrome.storage.local.set({
    skimStatus: "loading",
    skimOperation: operation,
    skimResult: null,
    skimError: null
  });

  chrome.action.setBadgeText({ text: "..." });
  chrome.action.setBadgeBackgroundColor({ color: "#6366F1" });

  try {
    const response = await fetch(BACKEND_URL, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ content, operation })
    });

    if (!response.ok) {
      throw new Error(`Server responded with status ${response.status}`);
    }

    const result = await response.text();

    await chrome.storage.local.set({
      skimStatus: "done",
      skimOperation: operation,
      skimResult: result,
      skimError: null
    });

    chrome.action.setBadgeText({ text: "" });

    // Try to auto-open the popup (works in newer Chrome versions from a user gesture context).
    // If it's not supported, fall back to a badge so the user knows to click the icon.
    try {
      await chrome.action.openPopup();
    } catch (e) {
      chrome.action.setBadgeText({ text: "1" });
      chrome.action.setBadgeBackgroundColor({ color: "#4F46E5" });
    }
  } catch (error) {
    await chrome.storage.local.set({
      skimStatus: "error",
      skimOperation: operation,
      skimResult: null,
      skimError: error.message || "Something went wrong"
    });
    chrome.action.setBadgeText({ text: "!" });
    chrome.action.setBadgeBackgroundColor({ color: "#DC2626" });
  }
});
