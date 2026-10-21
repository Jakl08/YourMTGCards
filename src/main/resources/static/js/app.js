// Zentraler Zustand: Welcher Scan gerade manuell mit einem Scryfall-Druck abgeglichen wird.
let selectedScanId = null;

// Kleine HTTP-Hilfe: JSON wird geparst und Backendfehler werden als lesbare Meldung weitergereicht.
async function requestJson(url, options = {}) {
  const response = await fetch(url, options);
  const payload = await response.json();
  if (!response.ok) {
    throw new Error(payload.message || `Anfrage fehlgeschlagen (${response.status}).`);
  }
  return payload;
}

// Baut Elemente mit textContent statt HTML-Strings, damit Kartennamen keinen Markup-Code einschleusen.
function makeTextElement(tagName, className, text) {
  const element = document.createElement(tagName);
  element.className = className;
  element.textContent = text;
  return element;
}

// Zeigt globale Erfolgs-, Lade- und Fehlermeldungen im barrierefrei angekündigten Statusfeld.
function showStatus(message, isError = false) {
  const status = document.querySelector("#app-status");
  status.textContent = message;
  status.classList.toggle("status-error", isError);
}

// Rendert einen ausstehenden Scan inklusive Foto, erkannter Hilfsdaten und Auswahlaktion.
function renderPendingCard(card) {
  const article = document.createElement("article");
  article.className = "card-tile";
  const image = document.createElement("img");
  image.className = "scan-image";
  image.src = card.scanImageUrl;
  image.alt = `Gescanntes Kartenbild, UUID ${card.uuid}`;
  article.append(image);

  const content = document.createElement("div");
  content.className = "tile-content";
  content.append(makeTextElement("h3", "tile-title", card.recognizedName || "Name nicht erkannt"));
  content.append(makeTextElement("p", "tile-detail", `Regal ${card.rackNumber} · Fach ${card.boxNumber}`));
  const button = makeTextElement("button", "button button-secondary", "Diesen Scan abgleichen");
  button.type = "button";
  button.dataset.scanId = card.uuid;
  button.dataset.scanName = card.recognizedName || "";
  button.classList.add("select-scan");
  content.append(button);
  article.append(content);
  return article;
}

// Lädt ausschließlich nicht bestätigte Scans und aktualisiert deren Auswahlbereich.
async function loadPendingCards() {
  const cards = await requestJson("/api/scans?status=PENDING");
  const list = document.querySelector("#pending-list");
  list.replaceChildren();
  if (cards.length === 0) {
    list.append(makeTextElement("p", "empty-state", "Keine unbestätigten Scans vorhanden."));
    return;
  }
  cards.forEach((card) => list.append(renderPendingCard(card)));
}

// Sucht über den Backend-Endpunkt nach unterschiedlichen Scryfall-Drucken und zeigt deren Artwork.
async function searchCandidates(query) {
  const candidates = await requestJson(`/api/cards/search?q=${encodeURIComponent(query)}`);
  const list = document.querySelector("#candidate-list");
  list.replaceChildren();
  if (candidates.length === 0) {
    list.append(makeTextElement("p", "empty-state", "Keine passenden Kartenausgaben gefunden."));
    return;
  }

  candidates.forEach((candidate) => {
    const article = document.createElement("article");
    article.className = "card-tile candidate-tile";
    if (candidate.imageUrl) {
      const image = document.createElement("img");
      image.className = "card-image";
      image.src = candidate.imageUrl;
      image.alt = `Artwork: ${candidate.name}, ${candidate.setName}`;
      image.loading = "lazy";
      article.append(image);
    }
    const content = document.createElement("div");
    content.className = "tile-content";
    content.append(makeTextElement("h3", "tile-title", candidate.name));
    content.append(makeTextElement("p", "tile-detail", `${candidate.setName} (${candidate.setCode.toUpperCase()}) · ${candidate.language}`));
    const button = makeTextElement("button", "button", "Ausgabe bestätigen");
    button.type = "button";
    button.dataset.scryfallId = candidate.id;
    button.classList.add("confirm-card");
    content.append(button);
    article.append(content);
    list.append(article);
  });
}

// Rendert einen Inventareintrag und bietet nur für bestätigte Exemplare eine Auslagerung an.
function renderInventoryCard(card) {
  const article = document.createElement("article");
  article.className = "inventory-row";
  const image = document.createElement("img");
  image.className = card.imageUrl ? "inventory-image" : "inventory-image scan-thumbnail";
  image.src = card.imageUrl || card.scanImageUrl;
  image.alt = card.name ? `Artwork: ${card.name}` : "Scanbild";
  image.loading = "lazy";
  article.append(image);

  const details = document.createElement("div");
  details.className = "inventory-details";
  details.append(makeTextElement("h3", "tile-title", card.name || card.recognizedName || "Unbestätigter Scan"));
  const setText = card.setCode ? ` · Set ${card.setCode.toUpperCase()}` : "";
  details.append(makeTextElement("p", "tile-detail", `Regal ${card.rackNumber} · Fach ${card.boxNumber}${setText}`));
  details.append(makeTextElement("span", "badge", card.status));
  article.append(details);

  if (card.status === "CONFIRMED") {
    const button = makeTextElement("button", "button button-secondary", "Auslagern");
    button.type = "button";
    button.dataset.cardId = card.uuid;
    button.classList.add("retrieve-card");
    article.append(button);
  }
  return article;
}

// Holt den gesamten Bestand oder eine gefilterte Trefferliste vom Backend.
async function loadInventory(query = "") {
  const suffix = query ? `?q=${encodeURIComponent(query)}` : "";
  const cards = await requestJson(`/api/scans${suffix}`);
  const list = document.querySelector("#inventory-list");
  list.replaceChildren();
  if (cards.length === 0) {
    list.append(makeTextElement("p", "empty-state", "Keine Karten für diese Suche gefunden."));
    return;
  }
  cards.forEach((card) => list.append(renderInventoryCard(card)));
}

// Auswahl eines Scans setzt den OCR-Text als Suchvorschlag, sofern ein solcher vorliegt.
document.querySelector("#pending-list").addEventListener("click", async (event) => {
  const button = event.target.closest(".select-scan");
  if (!button) return;
  selectedScanId = button.dataset.scanId;
  document.querySelector("#selected-scan").textContent = `Ausgewählter Scan: ${selectedScanId}`;
  if (button.dataset.scanName) {
    document.querySelector("#card-search").value = button.dataset.scanName;
    try {
      await searchCandidates(button.dataset.scanName);
    } catch (error) {
      showStatus(error.message, true);
    }
  }
});

// Übermittelt eine manuelle Kartensuche an Scryfall.
document.querySelector("#card-search-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    await searchCandidates(new FormData(event.currentTarget).get("q").toString());
    showStatus("Kartenausgaben wurden geladen.");
  } catch (error) {
    showStatus(error.message, true);
  }
});

// Bestätigt die gewählte Ausgabe nur, wenn zuvor ein konkreter physischer Scan ausgewählt wurde.
document.querySelector("#candidate-list").addEventListener("click", async (event) => {
  const button = event.target.closest(".confirm-card");
  if (!button) return;
  if (!selectedScanId) {
    showStatus("Bitte zuerst einen Scan auswählen.", true);
    return;
  }
  try {
    await requestJson(`/api/scans/${selectedScanId}/confirm`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ scryfallId: button.dataset.scryfallId }),
    });
    selectedScanId = null;
    document.querySelector("#selected-scan").textContent = "Wähle zuerst einen Scan, um ihn anschließend zu bestätigen.";
    document.querySelector("#candidate-list").replaceChildren();
    await Promise.all([loadPendingCards(), loadInventory()]);
    showStatus("Karte wurde bestätigt und dem Bestand hinzugefügt.");
  } catch (error) {
    showStatus(error.message, true);
  }
});

// Lädt nach einer Änderung des Suchfelds den Bestand mit optionalem Suchbegriff neu.
document.querySelector("#inventory-search-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    await loadInventory(new FormData(event.currentTarget).get("q").toString());
  } catch (error) {
    showStatus(error.message, true);
  }
});

// Fordert eine Karte an und informiert darüber, dass der Auftrag zunächst von der Pi-Software abgerufen wird.
document.querySelector("#inventory-list").addEventListener("click", async (event) => {
  const button = event.target.closest(".retrieve-card");
  if (!button) return;
  try {
    await requestJson(`/api/inventory/${button.dataset.cardId}/retrieval`, { method: "POST" });
    await loadInventory(document.querySelector("#inventory-search").value);
    showStatus("Auslagerungsauftrag wurde an die Hardware-Warteschlange übergeben.");
  } catch (error) {
    showStatus(error.message, true);
  }
});

// Manuelles Neuladen hält die Pending-Liste und den Bestand synchron.
document.querySelector("#refresh-button").addEventListener("click", async () => {
  try {
    await Promise.all([loadPendingCards(), loadInventory(document.querySelector("#inventory-search").value)]);
    showStatus("Daten wurden aktualisiert.");
  } catch (error) {
    showStatus(error.message, true);
  }
});

// Initialisiert beide Bereiche parallel und meldet einen Datenbank- oder Serverfehler sichtbar.
try {
  await Promise.all([loadPendingCards(), loadInventory()]);
  showStatus("Verbindung hergestellt. Bestand und Scans sind geladen.");
} catch (error) {
  showStatus(error.message, true);
}
