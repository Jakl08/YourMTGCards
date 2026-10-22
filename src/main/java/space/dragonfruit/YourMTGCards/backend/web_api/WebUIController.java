package space.dragonfruit.YourMTGCards.backend.web_api;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import space.dragonfruit.YourMTGCards.backend.datenbank.CardStatus;
import space.dragonfruit.YourMTGCards.backend.datenbank.PhysicalCard;
import space.dragonfruit.YourMTGCards.backend.logic.InventoryService;
import space.dragonfruit.YourMTGCards.backend.logic.ScanImageStorageService;
import space.dragonfruit.YourMTGCards.backend.physical_api.RetrievalCommandResponse;
import space.dragonfruit.YourMTGCards.backend.scryfall_api.CardCandidateResponse;
import space.dragonfruit.YourMTGCards.backend.scryfall_api.ScryfallService;

/** REST-Endpunkte für Suche, Pending-Abgleich, Bildanzeige und Auslagerung über die Weboberfläche. */
@RestController
@RequestMapping("/api")
public class WebUIController {

	private final InventoryService inventoryService;
	private final ScanImageStorageService imageStorageService;
	private final ScryfallService scryfallService;

	public WebUIController(InventoryService inventoryService,
			ScanImageStorageService imageStorageService,
			ScryfallService scryfallService) {
		this.inventoryService = inventoryService;
		this.imageStorageService = imageStorageService;
		this.scryfallService = scryfallService;
	}

	/** Liefert den Bestand gefiltert nach optionalem Status und Karten- oder OCR-Text. */
	@GetMapping("/scans")
	public List<InventoryCardResponse> listCards(
			@RequestParam(required = false) CardStatus status,
			@RequestParam(required = false) String q) {
		return inventoryService.listCards(status, q);
	}

	/** Sucht über Scryfall nach konkreten Drucken als Auswahl für den manuellen Artwork-Abgleich. */
	@GetMapping("/cards/search")
	public List<CardCandidateResponse> searchCards(@RequestParam String q) {
		if (q.isBlank()) {
			return List.of();
		}
		return scryfallService.search(q);
	}

	/** Bestätigt die gewählte Scryfall-Ausgabe und verknüpft sie mit dem physischen Exemplar. */
	@PostMapping("/scans/{uuid}/confirm")
	public InventoryCardResponse confirmScan(
			@PathVariable UUID uuid,
			@Valid @RequestBody ConfirmScanRequest request) {
		return inventoryService.confirmScan(uuid, request.scryfallId());
	}

	/** Liefert nur das Bild des angeforderten Scans aus, nicht den internen Speicherpfad. */
	@GetMapping("/scans/{uuid}/image")
	public ResponseEntity<ByteArrayResource> getScanImage(@PathVariable UUID uuid) {
		PhysicalCard card = inventoryService.getPhysicalCard(uuid);
		String filename = card.getScannedImagePath();
		MediaType mediaType = filename.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
		return ResponseEntity.ok()
				.contentType(mediaType)
				.cacheControl(CacheControl.noCache())
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + uuid + "\"")
				.body(new ByteArrayResource(imageStorageService.read(filename)));
	}

	/** Legt den Abrufauftrag an, den die Hardware anschließend über ihre Schnittstelle abholt. */
	@PostMapping("/inventory/{uuid}/retrieval")
	public ResponseEntity<RetrievalCommandResponse> requestRetrieval(@PathVariable UUID uuid) {
		return ResponseEntity.accepted().body(inventoryService.requestRetrieval(uuid));
	}
}
