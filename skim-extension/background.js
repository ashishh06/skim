const BACKEND_URL = "http://localhost:8080/api/skim/process";

const REWRITE_TONES = [
  { id: "formal", title: "Formal" },
  { id: "casual", title: "Casual" },
  { id: "fix-grammar", title: "Fix grammar" },
  { id: "shorten", title: "Shorten" },
  { id: "expand", title: "Expand" }
];

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
  chrome.contextMenus.create({
    id: "skim-explain",
    title: "Skim: Explain simply",
    contexts: ["selection"]
  });

  chrome.contextMenus.create({
    id: "skim-rewrite",
    title: "Skim: Rewrite as...",
    contexts: ["selection"]
  });
  REWRITE_TONES.forEach((tone) => {
    chrome.contextMenus.create({
      id: `skim-rewrite-${tone.id}`,
      parentId: "skim-rewrite",
      title: tone.title,
      contexts: ["selection"]
    });
  });
});

// Maps a clicked context menu id to { operation, tone? }
function resolveOperationFromMenuId(menuItemId) {
  if (menuItemId === "skim-summarize") return { operation: "summarize" };
  if (menuItemId === "skim-suggest") return { operation: "suggest" };
  if (menuItemId === "skim-explain") return { operation: "explain" };
  if (menuItemId.startsWith("skim-rewrite-")) {
    const tone = menuItemId.replace("skim-rewrite-", "");
    return { operation: "rewrite", tone };
  }
  return null;
}

chrome.contextMenus.onClicked.addListener(async (info) => {
  const resolved = resolveOperationFromMenuId(info.menuItemId);
  if (!resolved) return;

  const content = info.selectionText;
  if (!content) return;

  const { operation, tone } = resolved;

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
      body: JSON.stringify({ content, operation, tone })
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