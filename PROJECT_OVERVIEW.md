# Projektübersicht: YourMTGCards

Dieses Dokument erklärt, welche Dateien zum Projekt gehören, wo die Aufgaben
der Anwendung umgesetzt sind und wie die einzelnen Bereiche zusammenarbeiten.
Es ist als Wegweiser gedacht: Für API-Beispiele und Startbefehle siehe auch
[README.md](./README.md), für die ursprüngliche Anforderung [requirements_and_structure.md](./requirements_and_structure.md).

## 1. Projekt auf einen Blick

YourMTGCards besteht aus vier miteinander verbundenen Bereichen:

1. **Weboberfläche**: zeigt Scans und Bestand, sucht Karten und bestätigt den
   passenden Scryfall-Druck.
2. **Java-/Spring-Backend**: stellt die REST-Endpunkte bereit und setzt die
   Geschäftsregeln um.
3. **PostgreSQL-Datenbank und Bildspeicher**: halten Kartendaten, einzelne
   physische Exemplare, Auslagerungsaufträge und Scanbilder fest.
4. **Raspberry-Pi-Schnittstelle**: nimmt Scans entgegen und liefert der
   Hardware offene Auslagerungsaufträge.

Die Anwendung kann ohne angeschlossene Pi-Hardware entwickelt und gestartet
werden. OCR, Set-Icon-Erkennung und die eigentliche Motor-/Regalsteuerung sind
noch nicht implementiert.

## 2. Verzeichnisstruktur

```text
YourMTGCards/
├── .mvn/wrapper/                 Maven-Wrapper-Konfiguration
├── images/                       Architekturdiagramm (PNG und Quelldatei)
├── src/
│   ├── main/
│   │   ├── java/space/dragonfruit/YourMTGCards/
│   │   │   ├── YourMtgCardsApplication.java
│   │   │   └── backend/
│   │   │       ├── config/       Backend-Konfiguration
│   │   │       ├── controller/   REST-Endpunkte
│   │   │       ├── dto/          HTTP-Eingabe- und Ausgabeobjekte
│   │   │       ├── exception/    API-Fehlerbehandlung
│   │   │       ├── model/        Datenbankobjekte und Zustände
│   │   │       ├── repository/   Datenbankzugriff
│   │   │       └── service/      Geschäftslogik und externe Dienste
│   │   └── resources/
│   │       ├── application.yaml  Spring- und Datenbankkonfiguration
│   │       └── static/           Weboberfläche (HTML, CSS, JavaScript)
│   └── test/                     automatisierte Tests
├── docker-compose.yml            lokale PostgreSQL-Datenbank
├── pom.xml                       Java-Abhängigkeiten und Maven-Build
├── README.md                     Startanleitung und Pi-HTTP-Protokoll
└── requirements_and_structure.md ursprüngliche Anforderungen und Konzept
```

## 3. Ablauf der Anwendung

### 3.1 Scan empfangen

1. Die Pi-Software sendet UUID, Regalnummer, Fachnummer und optional
   `recognizedName` als JSON sowie das Scanbild als JPEG oder PNG an
   `POST /api/hardware/scans`.
2. [HardwareController.java](./src/main/java/space/dragonfruit/YourMTGCards/backend/controller/HardwareController.java)
   nimmt den Multipart-Request an.
3. [InventoryService.java](./src/main/java/space/dragonfruit/YourMTGCards/backend/service/InventoryService.java)
   prüft, dass die UUID neu ist, und legt das physische Exemplar im Status
   `PENDING` an.
4. [ScanImageStorageService.java](./src/main/java/space/dragonfruit/YourMTGCards/backend/service/ScanImageStorageService.java)
   prüft den Dateityp und speichert das Bild unter einem servergenerierten
   Dateinamen in `./data/scans` (konfigurierbar).

### 3.2 Scan mit einer Kartenausgabe abgleichen

1. Die Oberfläche lädt `PENDING`-Scans einschließlich ihres Scanbilds.
2. Der Benutzer wählt einen Scan. Falls ein erkannter Name vorliegt, wird
   dieser als Suchvorschlag eingesetzt.
3. Die Suche läuft über `GET /api/cards/search?q=...` zum Backend und von dort
   über [ScryfallService.java](./src/main/java/space/dragonfruit/YourMTGCards/backend/service/ScryfallService.java)
   zur Scryfall-API.
4. Der Benutzer wählt den passenden Druck anhand von Set, Sprache und Artwork.
5. `POST /api/scans/{uuid}/confirm` lässt das Backend die Druck-ID bei Scryfall
   prüfen. Das Backend speichert die Metadaten einmalig und verknüpft sie mit
   dem physischen Exemplar; dessen Status wird `CONFIRMED`.

### 3.3 Bestand durchsuchen und Karte auslagern

1. `GET /api/scans` liefert Exemplare zurück. Die optionale Suche `q` findet
   Treffer im erkannten Namen oder bestätigten Kartennamen; `status` filtert
   nach Zustand.
2. Der Benutzer fordert die Auslagerung eines bestätigten Exemplars über
   `POST /api/inventory/{uuid}/retrieval` an.
3. Das Backend setzt die Karte auf `RETRIEVAL_REQUESTED` und speichert einen
   Auftrag mit Status `QUEUED`.
4. Die Pi-Software fragt `GET /api/hardware/retrieval-commands` ab und erhält
   UUID sowie Regal- und Fachnummer.
5. Nach erfolgreicher physischer Entnahme quittiert sie über
   `POST /api/hardware/retrieval-commands/{commandId}/complete`. Der Auftrag
   wird `COMPLETED`, die Karte `RETRIEVED`.

### 3.4 Fehlerbehandlung

Controller und Services melden fachliche Fehler mit HTTP-Statuscodes.
[ApiExceptionHandler.java](./src/main/java/space/dragonfruit/YourMTGCards/backend/exception/ApiExceptionHandler.java)
formt fachliche Fehler, ungültige Eingaben und Fehler der externen Karten-API
in ein einheitliches JSON-Format um. Die Browseroberfläche zeigt die
Fehlermeldung im Statusbereich an.

## 4. Backend: Dateien und Aufgaben

Alle Java-Dateien liegen unter
`src/main/java/space/dragonfruit/YourMTGCards/`.

### Anwendungseinstieg

| Datei | Aufgabe |
|---|---|
| `YourMtgCardsApplication.java` | Startet Spring Boot. Spring durchsucht ab diesem Paket die Controller, Services, Entities und Repositories. |

### Controller: HTTP-Endpunkte

| Datei | Aufgabe |
|---|---|
| `backend/controller/HardwareController.java` | Pi-Endpunkte: Scan hochladen, offene Auslagerungsaufträge abholen und Auslagerung quittieren. |
| `backend/controller/WebUIController.java` | Browser-Endpunkte: Bestand und Scans laden, Scryfall-Drucke suchen, Scan bestätigen, Scanbild laden und Auslagerung anfordern. |
| `backend/controller/package-info.java` | Beschreibt das Controller-Paket; enthält selbst keine Endpunkte. |

### DTO: HTTP-Datenformate

DTO steht für „Data Transfer Object“. Diese Objekte bestimmen, welche Daten
über die API hereinkommen oder zurückgegeben werden. Sie sind nicht selbst die
Datenbanktabellen.

| Datei | Aufgabe |
|---|---|
| `backend/dto/ScanSubmissionRequest.java` | Eingabedaten eines Hardware-Scans: UUID, Regal, Fach und optional erkannter Name. |
| `backend/dto/ConfirmScanRequest.java` | Eingabe beim Bestätigen: die Scryfall-ID der ausgewählten Ausgabe. |
| `backend/dto/CardCandidateResponse.java` | Suchtreffer aus Scryfall mit Name, Set, Sprache und Bild-URL. |
| `backend/dto/InventoryCardResponse.java` | Sichere Darstellung eines physischen Exemplars für die Oberfläche; enthält keinen internen Dateipfad. |
| `backend/dto/RetrievalCommandResponse.java` | Auftrag für die Pi-Hardware: Auftrags-ID, Karten-UUID und Lagerposition. |
| `backend/dto/ApiErrorResponse.java` | Einheitliches Format für API-Fehler. |

### Geschäftslogik und Integrationen

| Datei | Aufgabe |
|---|---|
| `backend/service/InventoryService.java` | Zentrale Fachlogik für Scans, Suche im Bestand, Bestätigung, Statuswechsel und Auslagerungsaufträge. |
| `backend/service/ScanImageStorageService.java` | Validiert JPEG/PNG, speichert Scanbilder und liest sie für den Bild-Endpunkt aus. |
| `backend/service/ScryfallService.java` | Ruft Scryfall-Suchergebnisse und einzelne Drucke ab und wandelt sie in API-Antworten um. |
| `backend/service/package-info.java` | Beschreibt das Service-Paket; enthält selbst keine Geschäftslogik. |

### Datenbankzugriff

| Datei | Aufgabe |
|---|---|
| `backend/repository/CardMetadataRepository.java` | Sucht und speichert abstrakte Kartendaten über ihre Scryfall-ID. |
| `backend/repository/PhysicalCardRepository.java` | Sucht und speichert physische Exemplare. |
| `backend/repository/RetrievalCommandRepository.java` | Sucht und speichert Auslagerungsaufträge. |
| `backend/repository/package-info.java` | Beschreibt das Repository-Paket. |

Spring Data JPA erstellt die Standardoperationen zum Lesen und Schreiben. Die
Methodennamen in den Repository-Interfaces beschreiben zusätzliche Suchabfragen.

### Datenbankmodelle und Statuswerte

| Datei | Datenbankobjekt / Bedeutung |
|---|---|
| `backend/model/CardMetadata.java` | Tabelle `card_metadata`: einmalige Daten eines konkreten Scryfall-Drucks (ID, Name, Set, Sprache, Bild-URL). |
| `backend/model/PhysicalCard.java` | Tabelle `physical_inventory`: einzelnes Exemplar, UUID, Lagerposition, Scanbild, erkannter Name, Status und Verknüpfung zu den Metadaten. |
| `backend/model/RetrievalCommand.java` | Tabelle `retrieval_commands`: persistenter Auslagerungsauftrag zu einem physischen Exemplar. |
| `backend/model/CardStatus.java` | Zustände des Exemplars: `PENDING`, `CONFIRMED`, `RETRIEVAL_REQUESTED`, `RETRIEVED`. |
| `backend/model/CommandStatus.java` | Zustände des Hardware-Auftrags: `QUEUED`, `COMPLETED`. |
| `backend/model/package-info.java` | Beschreibt das Model-Paket. |
| `backend/config/package-info.java` | Markiert den Paketbereich für Konfigurationsklassen; dort liegt derzeit keine eigene Konfigurationsklasse. |

`CardMetadata` und `PhysicalCard` sind getrennt, damit viele physische Exemplare
auf denselben Kartendruck verweisen können, ohne dieselben Kartendaten mehrfach
zu speichern.

### Fehlerbehandlung

| Datei | Aufgabe |
|---|---|
| `backend/exception/ApiException.java` | Fachlicher Fehler mit passendem HTTP-Status und verständlicher Meldung. |
| `backend/exception/ApiExceptionHandler.java` | Wandelt fachliche Fehler, Validierungsfehler und Scryfall-Verbindungsfehler in JSON-Antworten um. |
| `backend/exception/package-info.java` | Beschreibt das Exception-Paket. |

## 5. Weboberfläche: Dateien und Aufgaben

| Datei | Aufgabe |
|---|---|
| `src/main/resources/static/index.html` | Struktur der Seite: Kopfbereich, Status, Pending-Scans, Scryfall-Suche und Bestand. Enthält die IDs, an die JavaScript seine Bedienaktionen bindet. |
| `src/main/resources/static/js/app.js` | Lädt API-Daten, rendert Karten und verarbeitet Auswahl, Suche, Bestätigung, Auslagerung und Fehleranzeigen. |
| `src/main/resources/static/css/styles.css` | Farben, Layout, Buttons, Karten-/Bestandsdarstellung, Fokuszustände und Anpassung an schmale Bildschirme. |

### JavaScript-Ablauf in `app.js`

1. `requestJson` kommuniziert mit der Backend-API und behandelt Fehler.
2. `makeTextElement` erzeugt Text-Elemente ohne Kartennamen als HTML zu
   interpretieren.
3. `showStatus` zeigt Lade-, Erfolgs- und Fehlermeldungen.
4. `renderPendingCard`, `searchCandidates` und `renderInventoryCard` bauen die
   sichtbaren Kartenkacheln aus API-Antworten.
5. `loadPendingCards` und `loadInventory` laden die jeweiligen Listen.
6. Event-Handler verbinden Formular- und Button-Aktionen mit den API-Endpunkten.
7. Am Ende werden Pending-Scans und Bestand beim Seitenaufruf geladen.

## 6. Konfiguration, Build und lokale Dateien

| Datei | Aufgabe |
|---|---|
| `pom.xml` | Maven-Projektdefinition: Java-/Spring-Boot-Version, Abhängigkeiten und Build-Plugin. |
| `.mvn/wrapper/maven-wrapper.properties` | Legt die Maven-Version fest, die `mvnw` automatisch herunterlädt und verwendet. |
| `mvnw`, `mvnw.cmd` | Startskripte des Maven Wrappers für Unix/macOS/Linux beziehungsweise Windows. |
| `src/main/resources/application.yaml` | Spring-Anwendungsname, Datenbankverbindung, JPA-Schemaabgleich, Upload-Limits, Bildverzeichnis und Scryfall-Adresse. |
| `docker-compose.yml` | Startet PostgreSQL lokal mit persistentem Volume und Gesundheitsprüfung. |
| `src/test/java/space/dragonfruit/YourMTGCards/YourMtgCardsApplicationTests.java` | Prüft den Spring-Kontext und das Speichern eines neuen Exemplars im Status `PENDING`; verwendet H2 statt PostgreSQL. |
| `.vscode/settings.json` | Projektbezogene Einstellungen für Visual Studio Code. |

Beim lokalen Start werden Scanbilder standardmäßig unter `./data/scans`
abgelegt. Dieser Ordner ist kein Teil der Webdateien. Datenbankadresse und
Zugangsdaten lassen sich über Umgebungsvariablen überschreiben; Details stehen
in der README.

## 7. Sonstige Projektdateien

| Datei | Aufgabe |
|---|---|
| `README.md` | Einstieg: Starten, Testen, Konfiguration und konkrete Beispiele des Pi-HTTP-Protokolls. |
| `requirements_and_structure.md` | Ursprüngliche fachliche Anforderungen und Architekturidee; beschreibt teils geplante Funktionen, die noch nicht implementiert sind. |
| `HELP.md` | Von der Spring-Boot-Projektvorlage erzeugte Links zu Referenzdokumentation und Guides. |
| `images/YourMTGCards.png` | Visuelle Darstellung des Architekturkonzepts; in der README eingebunden. |
| `images/YourMTGCards.xml` | Quelldatei des Architekturdiagramms. |

### Maven-Ausgabe (`target/`)

`target/` enthält von Maven erzeugte Dateien, zum Beispiel kompilierte
`.class`-Dateien, kopierte Webressourcen, Testberichte und Build-Metadaten.
Diese Dateien werden bei einem Build neu erzeugt. Änderungen sollten deshalb
an den Quelldateien unter `src/` oder an der Maven-Konfiguration erfolgen,
nicht direkt in `target/`.

## 8. Welche Datei öffne ich für welche Änderung?

| Ich möchte … | Hier anfangen |
|---|---|
| einen API-Endpunkt ergänzen | passender Controller unter `backend/controller/` |
| Geschäftsregeln oder Statusübergänge ändern | `backend/service/InventoryService.java` und gegebenenfalls `backend/model/` |
| Tabellen oder Beziehungen ändern | `backend/model/`, danach Tests prüfen |
| neue Datenbankabfrage ergänzen | passendes Interface unter `backend/repository/` |
| ein JSON-Format ändern | passendes Record unter `backend/dto/` |
| Fehler einheitlich zurückgeben | `backend/exception/` |
| mit Scryfall kommunizieren | `backend/service/ScryfallService.java` |
| Bildspeicherung ändern | `backend/service/ScanImageStorageService.java` und `application.yaml` |
| Text, Bereiche oder Formulare der Seite ändern | `static/index.html` |
| Verhalten, Laden oder Klickaktionen ändern | `static/js/app.js` |
| Aussehen oder mobile Darstellung ändern | `static/css/styles.css` |
| Datenbank lokal starten oder konfigurieren | `docker-compose.yml` und `application.yaml` |
| Hardware-Protokoll oder Startanleitung nachschlagen | `README.md` |
| Tests hinzufügen oder ändern | `src/test/java/` |

## 9. Bewusste Grenzen des aktuellen MVP

- **Noch keine OCR/Bilderkennung:** `recognizedName` kann optional von der
  Pi-Software geliefert werden; andernfalls wählt der Benutzer die Karte
  manuell aus.
- **Noch keine eigentliche Hardwaresteuerung:** Das Backend speichert
  Auslagerungsaufträge und bietet sie der Pi-Software an, steuert aber keine
  Motoren oder Sensoren.
- **Noch keine API-Authentifizierung:** Die Hardware-Endpunkte sind im MVP
  nicht geschützt. Das System daher nur in einem vertrauenswürdigen Netzwerk
  betreiben.
- **Schemaänderungen:** Hibernate aktualisiert das Schema für den MVP
  automatisch. Vor einem produktiven Betrieb sollten Datenbankmigrationen
  eingerichtet und die Standard-Zugangsdaten ersetzt werden.
