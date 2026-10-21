# Architekturkonzept: Automatisches MTG-Kartensortiersystem

## 1. Anforderungen

* Das System muss Magic-Karten physisch von einem Stapel nehmen, einscannen und in ein spezifisches Hochregal einsortieren.
* Die Maschine weist jedem gescannten physischen Exemplar eine eindeutige UUID zu und sendet diese zusammen mit der genauen Lagerposition an das Backend.
* Das Backend muss in der Lage sein, auf dem gescannten Bild Merkmale wie den Kartennamen, das Set-Icon und die Sprache maschinell zu erkennen.
* Eine Webapplikation dient als Benutzeroberfläche, um gescannte Karten mit den Einträgen einer öffentlichen MTG-Drittanbieter-API abzugleichen und das korrekte Artwork zu bestätigen.
* Die relationale Datenbank speichert die Bestände effizient und ohne unnötige Duplikate, wobei abstrakte Kartendaten von den physischen Lagerdaten getrennt werden (ähnlich dem Prinzip, das du vielleicht schon in der Datei YourMTGCards verfolgst).
* Über das Webinterface kann der Benutzer seinen gesamten Bestand einsehen, durchsuchen und der Maschine den Befehl erteilen, spezifische Karten wieder aus dem Regal auszulagern.

## 2. Datenbank Struktur

Um Datenredundanz zu vermeiden, wird eine relationale Datenbank (z.B. PostgreSQL oder MySQL) verwendet, die in mindestens zwei Haupttabellen unterteilt ist:

**Tabelle: `Card_Metadata` (Abstrakte Kartendaten)**
Speichert die allgemeinen Informationen einer Karte aus der MTG-API genau ein einziges Mal.
* `id` (Primary Key, z.B. Scryfall-ID)
* `name` (Name der Karte)
* `set_code` (Set-Kürzel)
* `language` (Sprache)
* `image_url` (URL zum offiziellen API-Bild)

**Tabelle: `Physical_Inventory` (Deine physischen Karten)**
Speichert jedes einzelne Exemplar, das im Regal liegt.
* `uuid` (Primary Key, generiert beim Scan)
* `card_metadata_id` (Foreign Key -> Verweis auf `Card_Metadata`)
* `rack_number` (Nummer/ID des Hochregals)
* `box_number` (Nummer der Kiste/des Fachs)
* `scanned_image_path` (Lokaler Dateipfad zum Foto des Scanners)
* `status` (z.B. "PENDING", "CONFIRMED")

## 3. Backend Struktur

Das Backend wird in Java (beispielsweise mit Spring Boot) entwickelt und folgt einer klassischen Schichtenarchitektur:

**A. Controller Layer (REST-APIs)**
* `HardwareController`: Nimmt Daten der Maschine entgegen (Scan-Bilder, UUIDs, Lagerpositionen) und sendet Auslagerungsbefehle an den Maschinen-Controller.
* `WebUIController`: Stellt Endpunkte für die Webapplikation bereit (Inventar abrufen, unbestätigte Scans auflisten, Karte bestätigen).

**B. Service Layer (Geschäftslogik)**
* `ScanProcessingService`: Verarbeitet eingehende Bilder, speichert sie lokal und nutzt Bilderkennung/OCR zur Identifikation von Kartennamen und Set-Symbolen.
* `MTGApiService`: Kommuniziert mit der externen MTG-API (z.B. Scryfall), um Metadaten und verschiedene Kartenversionen basierend auf dem erkannten Text abzurufen.
* `InventoryService`: Verwaltet die Verknüpfung zwischen gescannten Karten und den bestätigten API-Daten.

**C. Repository Layer (Data Access)**
* Nutzt Frameworks wie JPA/Hibernate, um die Java-Objekte in die Tabellen `Card_Metadata` und `Physical_Inventory` zu persistieren.

## 4. Workflow

1. **Einscannen & Lagern:** Die Maschine scannt eine Karte, generiert eine UUID, legt die Karte im Regal ab und sendet die UUID, das Bild, die Regalnummer und die Kistennummer an das Java-Backend.
2. **Bildverarbeitung:** Das Backend speichert das gescannte Bild lokal ab und wendet OCR/Bilderkennung an, um den Namen (und bestenfalls das Set) vorläufig zu identifizieren.
3. **Zwischenspeicherung:** Der Datensatz wird als "PENDING" in der `Physical_Inventory` Tabelle gespeichert, wobei der erkannte Name als Hilfswert hinterlegt wird.
4. **Benutzer-Abgleich (Frontend):** Der Nutzer öffnet die Web-App und lässt sich die "PENDING"-Karten anzeigen; er sieht das lokal gescannte Foto neben den möglichen offiziellen Versionen, die das Backend über die MTG-API geladen hat.
5. **Bestätigung:** Der Nutzer wählt die exakt passende Version (korrektes Set/Artwork) in der UI aus und bestätigt die Zuordnung.
6. **Finalisierung:** Das Backend prüft, ob diese Karte bereits in `Card_Metadata` existiert (legt sie falls nötig an) und verknüpft dann den `Physical_Inventory`-Eintrag dauerhaft mit dieser ID, woraufhin der Status auf "CONFIRMED" gesetzt wird.