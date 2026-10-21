package space.dragonfruit.YourMTGCards.backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import space.dragonfruit.YourMTGCards.backend.dto.CardCandidateResponse;
import space.dragonfruit.YourMTGCards.backend.exception.ApiException;

/** Ruft Kartensuche und einzelne Drucke über die öffentliche Scryfall-API ab. */
@Service
public class ScryfallService {

	private final RestClient restClient;
	private final String baseUrl;

	public ScryfallService(@Value("${yourmtgcards.scryfall.base-url}") String baseUrl) {
		this.baseUrl = baseUrl;
		this.restClient = RestClient.builder()
				.defaultHeader("User-Agent", "YourMTGCards/1.0 (card inventory application)")
				.build();
	}

	/** Sucht Drucke statt nur eindeutiger Kartennamen, damit Artwork und Set verglichen werden können. */
	public List<CardCandidateResponse> search(String query) {
		String uri = UriComponentsBuilder.fromUriString(baseUrl + "/cards/search")
				.queryParam("q", query)
				.queryParam("unique", "prints")
				.queryParam("order", "released")
				.queryParam("dir", "desc")
				.build()
				.encode()
				.toUriString();
		JsonNode result = restClient.get()
				.uri(uri)
				.retrieve()
				// Scryfall verwendet 404 auch für eine gültige Suche ohne Treffer.
				.onStatus(status -> status.value() == 404, (request, response) -> {
				})
				.body(JsonNode.class);
		if (result == null || !result.path("data").isArray()) {
			return List.of();
		}
		List<CardCandidateResponse> candidates = new ArrayList<>();
		for (JsonNode card : result.path("data")) {
			candidates.add(toCandidate(card));
		}
		return candidates;
	}

	/** Lädt eine konkrete Druckversion nach ihrer Scryfall-ID, bevor sie dauerhaft bestätigt wird. */
	public CardCandidateResponse getById(UUID id) {
		JsonNode card = restClient.get()
				.uri(baseUrl + "/cards/" + id)
				.retrieve()
				.onStatus(status -> status.value() == 404,
						(request, response) -> {
							throw new ApiException(HttpStatus.NOT_FOUND, "Die Scryfall-Karten-ID wurde nicht gefunden.");
						})
				.body(JsonNode.class);
		if (card == null) {
			throw new ApiException(HttpStatus.BAD_GATEWAY, "Scryfall lieferte keine Kartendaten.");
		}
		return toCandidate(card);
	}

	private CardCandidateResponse toCandidate(JsonNode card) {
		String imageUrl = card.path("image_uris").path("normal").asText(null);
		if (imageUrl == null && card.path("card_faces").isArray() && !card.path("card_faces").isEmpty()) {
			imageUrl = card.path("card_faces").get(0).path("image_uris").path("normal").asText(null);
		}
		return new CardCandidateResponse(
				UUID.fromString(card.path("id").asText()),
				card.path("name").asText(),
				card.path("set").asText(),
				card.path("set_name").asText(),
				card.path("lang").asText(),
				imageUrl);
	}
}
