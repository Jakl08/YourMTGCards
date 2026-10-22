package space.dragonfruit.YourMTGCards.backend.web_api;

import java.time.Instant;

/** Einheitliches Fehlerformat für Weboberfläche und Pi-Client. */
public record ApiErrorResponse(Instant timestamp, int status, String error, String message) {
}
