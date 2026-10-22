package space.dragonfruit.YourMTGCards.backend.web_api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Auswahl der eindeutig passenden Scryfall-Ausgabe für einen Scan. */
public record ConfirmScanRequest(@NotNull UUID scryfallId) {
}
