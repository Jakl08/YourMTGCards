# Projektübersicht: YourMTGCards

Dieses Dokument erklärt, welche Dateien zum Projekt gehören, wo Aufgaben
umgesetzt sind und wie die Bereiche zusammenarbeiten. Startbefehle und
konkrete HTTP-Beispiele stehen in der [README.md](./README.md); das ursprüngliche
Konzept steht in [requirements_and_structure.md](./requirements_and_structure.md).

## 1. Gesamtstruktur

```text
YourMTGCards/
├── .mvn/wrapper/                 Maven-Wrapper-Konfiguration
├── images/                       Architekturdiagramm und Quelldatei
├── src/
│   ├── main/
│   │   ├── java/space/dragonfruit/YourMTGCards/
│   │   │   ├── YourMtgCardsApplication.java
│   │   │   └── backend/
│   │   │       ├── scryfall_api/ Scryfall-Integration
│   │   │       ├── datenbank/    Datenbankobjekte und Datenzugriff
│   │   │       ├── web_api/      API für Browser/Weboberfläche
│   │   │       ├── physical_api/ API für Pi-Hardware
│   │   │       └── logic/        Geschäftsabläufe und Backend-Services
│   │   └── resources/
│   │       ├── application.yaml  Konfiguration
│   │       └── static/           HTML, CSS und JavaScript der Weboberfläche
│   └── test/                     automatisierte Tests
├── docker-compose.yml            lokale PostgreSQL-Datenbank
├── pom.xml                       Abhängigkeiten und Maven-Build
├── README.md                     Startanleitung und Pi-Protokoll
└── requirements_and_structure.md ursprüngliches Konzept
```

Die fünf Ordner unter `backend/` sind nach ihrer Aufgabe getrennt:

| Ordner | Zuständigkeit |
|---|---|
| `scryfall_api/` | Suche in Scryfall, Abruf einzelner Drucke und Ergebnisformat für Kartenkandidaten. |
| `datenbank/` | JPA-Entitäten, Statuswerte und Spring-Data-Repositories. |
| `web_api/` | REST-Endpunkte und Ein-/Ausgabeobjekte für die Browser-Weboberfläche sowie API-Fehlerbehandlung. |
| `physical_api/` | REST-Endpunkte und Ein-/Ausgabeobjekte für die Raspberry-Pi-Hardware. |
| `logic/` | Fachabläufe, die API-Eingaben verarbeiten, Datenbankoperationen koordinieren und Scanbilder verwalten. |

## 2. Ablauf: vom Scan bis zur Auslagerung

### 2.1 Scan empfangen

1. Die Pi-Software sendet UUID, Regal, Fach und optional `recognizedName` als
   JSON sowie ein JPEG-/PNG-Bild an `POST /api/hardware/scans`.
2. `physical_api/HardwareController.java` nimmt die Anfrage an und delegiert
   sie an die Geschäftslogik.
3. `logic/InventoryService.java` prüft, dass die UUID neu ist, speichert das
   Bild und legt ein Exemplar im Status `PENDING` in der Datenbank an.
4. `logic/ScanImageStorageService.java` validiert das Bild und speichert es
   standardmäßig außerhalb des Webroots unter `./data/scans`.

### 2.2 Kartenausgabe bestätigen

1. Die Weboberfläche lädt die Scans mit Status `PENDING` und zeigt die
   Scanbilder an.
2. Der Benutzer wählt einen Scan. Ein vom Pi mitgesendeter erkannter Name
   kann als Suchvorschlag verwendet werden.
3. `web_api/WebUIController.java` leitet die Suche an
   `scryfall_api/ScryfallService.java` weiter, der die Scryfall-API abfragt.
4. Der Benutzer wählt anhand von Set, Sprache und Artwork die passende Ausgabe.
5. Das Backend prüft die Scryfall-ID, speichert die Kartendaten einmalig in
   `datenbank/CardMetadata.java` und verknüpft den Scan damit. Der Status des
   Exemplars wechselt zu `CONFIRMED`.

### 2.3 Bestand durchsuchen und Karte auslagern

1. `GET /api/scans` liefert Exemplare zurück. `q` sucht im erkannten oder
   bestätigten Namen; `status` filtert nach Zustand.
2. Die Weboberfläche fordert mit `POST /api/inventory/{uuid}/retrieval` die
   Auslagerung eines bestätigten Exemplars an.
3. `logic/InventoryService.java` wechselt den Kartenstatus zu
   `RETRIEVAL_REQUESTED` und legt einen Auftrag mit Status `QUEUED` an.
4. Die Pi-Software fragt `GET /api/hardware/retrieval-commands` ab und erhält
   Auftrags-ID sowie Regal- und Fachnummer.
5. Nach der physischen Entnahme quittiert sie mit
   `POST /api/hardware/retrieval-commands/{commandId}/complete`. Der Auftrag
   wechselt auf `COMPLETED`, das Exemplar auf `RETRIEVED`.

## 3. Dateien in `backend/`

Alle folgenden Pfade sind relativ zu
`src/main/java/space/dragonfruit/YourMTGCards/backend/`.

### `scryfall_api/` — externe Kartendaten

| Datei | Aufgabe |
|---|---|
| `ScryfallService.java` | Ruft Scryfall-Suchergebnisse und einzelne Drucke ab und wandelt die JSON-Antworten in interne Ergebnisobjekte um. |
| `CardCandidateResponse.java` | Kompaktes Ergebnis für die Weboberfläche: Karten-ID, Name, Set, Sprache und Artwork-URL. |
| `package-info.java` | Beschreibt die Scryfall-Integration als Java-Paket. |

### `datenbank/` — Entitäten und Datenbankzugriff

| Datei | Aufgabe |
|---|---|
| `CardMetadata.java` | Entität der Tabelle `card_metadata`; speichert einen konkreten Kartendruck anhand der Scryfall-ID einmalig. |
| `PhysicalCard.java` | Entität der Tabelle `physical_inventory`; speichert ein physisches Exemplar, Position, Scanbild, Status und Metadaten-Verknüpfung. |
| `RetrievalCommand.java` | Entität der Tabelle `retrieval_commands`; speichert einen Hardware-Auslagerungsauftrag. |
| `CardStatus.java` | Status eines Exemplars: `PENDING`, `CONFIRMED`, `RETRIEVAL_REQUESTED` oder `RETRIEVED`. |
| `CommandStatus.java` | Status eines Auftrags: `QUEUED` oder `COMPLETED`. |
| `CardMetadataRepository.java` | Sucht und speichert Kartendaten nach ihrer Scryfall-ID. |
| `PhysicalCardRepository.java` | Sucht und speichert physische Exemplare, unter anderem nach Name und Status. |
| `RetrievalCommandRepository.java` | Sucht und speichert Auslagerungsaufträge nach ihrem Zustand. |
| `package-info.java` | Beschreibt das Datenbankpaket. |

`CardMetadata` und `PhysicalCard` sind getrennt: Mehrere physische Exemplare
können auf denselben Kartendruck verweisen, ohne seine Metadaten zu duplizieren.

### `web_api/` — API für die Browser-Weboberfläche

| Datei | Aufgabe |
|---|---|
| `WebUIController.java` | Endpunkte für Bestand, Scryfall-Kartensuche, Scanbestätigung, Bildauslieferung und Auslagerungsanforderung. |
| `ConfirmScanRequest.java` | Eingabeobjekt mit der vom Benutzer gewählten Scryfall-ID. |
| `InventoryCardResponse.java` | Ausgabeobjekt für ein Exemplar; enthält einen Bild-Endpunkt statt eines lokalen Dateipfads. |
| `ApiErrorResponse.java` | Einheitliches JSON-Format für Fehlerantworten. |
| `ApiException.java` | Fachlicher Fehler mit HTTP-Status und lesbarer Meldung. |
| `ApiExceptionHandler.java` | Übersetzt Fach-, Validierungs- und Scryfall-Fehler in HTTP-Fehlerantworten. |
| `package-info.java` | Beschreibt die Web-API als Java-Paket. |

### `physical_api/` — API für die Raspberry-Pi-Hardware

| Datei | Aufgabe |
|---|---|
| `HardwareController.java` | Endpunkte zum Hochladen eines Scans, Abholen offener Aufträge und Quittieren einer Auslagerung. |
| `ScanSubmissionRequest.java` | JSON-Eingabe mit UUID, Regalnummer, Fachnummer und optional erkanntem Kartennamen. |
| `RetrievalCommandResponse.java` | Auftragsantwort mit Auftrags-ID, Karten-UUID und Lagerposition. |
| `package-info.java` | Beschreibt die Hardware-API als Java-Paket. |

### `logic/` — Geschäftsabläufe

| Datei | Aufgabe |
|---|---|
| `InventoryService.java` | Zentrale Fachlogik für Scanannahme, Suche, Bestätigung, Statuswechsel und Auslagerungsaufträge. |
| `ScanImageStorageService.java` | Prüft JPEG-/PNG-Uploads und speichert beziehungsweise liest Bilder außerhalb des Webroots. |
| `package-info.java` | Beschreibt das Paket für Geschäftslogik und interne Services. |

## 4. Weboberfläche

Die Oberfläche liegt unter `src/main/resources/static/`:

| Datei | Aufgabe |
|---|---|
| `index.html` | Seitenstruktur mit Bereichen für unbestätigte Scans, Scryfall-Suche und Bestand. |
| `js/app.js` | Lädt API-Daten, rendert Karten und behandelt Suche, Bestätigung, Auslagerung und Fehlermeldungen. |
| `css/styles.css` | Farben, Layout, Kartenkacheln, Buttons, Fokusdarstellung und mobile Ansicht. |

`app.js` initialisiert die Listen beim Seitenstart. Anschließend verbinden
Event-Handler die Formulare und Buttons mit den Endpunkten im Backend. Texte
aus Karten- und API-Daten werden als Text dargestellt, nicht als HTML
interpretiert.

## 5. Konfiguration, Tests und weitere Projektdateien

| Datei | Aufgabe |
|---|---|
| `src/main/java/space/dragonfruit/YourMTGCards/YourMtgCardsApplication.java` | Startpunkt; aktiviert Spring Boot und den automatischen Scan der Backend-Pakete. |
| `src/main/resources/application.yaml` | Datenbankadresse, JPA-Schemaabgleich, Upload-Grenzen, Bildverzeichnis und Scryfall-Basisadresse. |
| `docker-compose.yml` | Startet PostgreSQL lokal und legt Daten in einem persistenten Volume ab. |
| `pom.xml` | Java-/Spring-Boot-Version, Bibliotheken und Build-Konfiguration für Maven. |
| `.mvn/wrapper/maven-wrapper.properties` | Legt die Maven-Version für den Wrapper fest. |
| `mvnw`, `mvnw.cmd` | Maven-Wrapper-Startskripte für Unix/macOS/Linux beziehungsweise Windows. |
| `src/test/java/space/dragonfruit/YourMTGCards/YourMtgCardsApplicationTests.java` | Prüft den Spring-Kontext und das Speichern eines neuen Exemplars mit Status `PENDING`; verwendet H2. |
| `README.md` | Installations-/Startbefehle und konkretes HTTP-Protokoll für den Raspberry Pi. |
| `requirements_and_structure.md` | Ursprüngliche Anforderungen und fachliches Konzept. |
| `HELP.md` | Von der Spring-Boot-Vorlage erzeugte Links zu Referenzdokumentation. |
| `images/YourMTGCards.png`, `images/YourMTGCards.xml` | Architekturdiagramm und Quelldatei. |
| `.vscode/settings.json` | Projekteinstellungen für Visual Studio Code. |

`target/` enthält generierte Maven-Ausgaben wie kompilierte Klassen, kopierte
Webdateien und Testberichte. Änderungen werden in `src/`, `pom.xml` oder den
Konfigurationsdateien vorgenommen, nicht direkt in `target/`.

## 6. Wo ändere ich etwas?

| Änderung | Einstiegspunkt |
|---|---|
| Scryfall-Suche oder Scryfall-Antworten | `backend/scryfall_api/` |
| Datenbanktabellen oder Beziehungen | `backend/datenbank/` |
| Datenbankabfragen | `backend/datenbank/*Repository.java` |
| Browser-API oder deren JSON-Formate | `backend/web_api/` |
| Pi-Protokoll oder Hardware-JSON-Formate | `backend/physical_api/` |
| Fachregeln und Statusübergänge | `backend/logic/InventoryService.java` und `backend/datenbank/` |
| Bildvalidierung und -speicherung | `backend/logic/ScanImageStorageService.java` und `application.yaml` |
| Seitentext und HTML-Struktur | `static/index.html` |
| Verhalten und Klickaktionen im Browser | `static/js/app.js` |
| Layout und Darstellung | `static/css/styles.css` |
| PostgreSQL lokal oder Verbindungswerte | `docker-compose.yml` und `application.yaml` |
| Testfälle | `src/test/java/` |

## 7. Grenzen des aktuellen MVP

- OCR und Erkennung von Kartennamen oder Set-Symbolen sind nicht implementiert.
  Ein erkannter Name kann optional von der Pi-Software mitgeschickt werden.
- Die Pi-Software sowie Motoren- und Sensorsteuerung sind nicht Teil dieses
  Backends; es stellt nur die HTTP-Schnittstelle und persistente Aufträge bereit.
- Hardware-Endpunkte sind noch nicht authentifiziert. Die Anwendung daher nur
  in einem vertrauenswürdigen lokalen Netzwerk betreiben.
- Hibernate aktualisiert das Datenbankschema im MVP automatisch. Vor einem
  Produktivbetrieb sollten Migrationen und sichere Zugangsdaten eingerichtet
  werden.
