package space.dragonfruit.YourMTGCards.backend.scryfall_api;

import java.util.UUID;

/** Sichere, kompakte Kartenansicht, die aus der Scryfall-Antwort für die Weboberfläche entsteht. */
public record CardCandidateResponse(
		UUID id,
		String name,
		String setCode,
		String setName,
		String language,
		String imageUrl) {
}
