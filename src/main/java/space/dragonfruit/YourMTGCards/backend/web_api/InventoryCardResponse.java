package space.dragonfruit.YourMTGCards.backend.web_api;

import java.time.Instant;
import java.util.UUID;
import space.dragonfruit.YourMTGCards.backend.datenbank.CardStatus;

/** Inventareintrag ohne lokale Dateipfade oder interne JPA-Details. */
public record InventoryCardResponse(
		UUID uuid,
		String rackNumber,
		String boxNumber,
		String recognizedName,
		String name,
		String setCode,
		String language,
		String imageUrl,
		String scanImageUrl,
		CardStatus status,
		Instant scannedAt) {
}
