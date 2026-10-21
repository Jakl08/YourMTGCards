package space.dragonfruit.YourMTGCards.backend.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import space.dragonfruit.YourMTGCards.backend.dto.CardCandidateResponse;
import space.dragonfruit.YourMTGCards.backend.dto.InventoryCardResponse;
import space.dragonfruit.YourMTGCards.backend.dto.RetrievalCommandResponse;
import space.dragonfruit.YourMTGCards.backend.dto.ScanSubmissionRequest;
import space.dragonfruit.YourMTGCards.backend.exception.ApiException;
import space.dragonfruit.YourMTGCards.backend.model.CardMetadata;
import space.dragonfruit.YourMTGCards.backend.model.CardStatus;
import space.dragonfruit.YourMTGCards.backend.model.CommandStatus;
import space.dragonfruit.YourMTGCards.backend.model.PhysicalCard;
import space.dragonfruit.YourMTGCards.backend.model.RetrievalCommand;
import space.dragonfruit.YourMTGCards.backend.repository.CardMetadataRepository;
import space.dragonfruit.YourMTGCards.backend.repository.PhysicalCardRepository;
import space.dragonfruit.YourMTGCards.backend.repository.RetrievalCommandRepository;

/** Geschäftslogik für Scanannahme, Bestandsabgleich und persistente Auslagerungsaufträge. */
@Service
public class InventoryService {

	private final CardMetadataRepository metadataRepository;
	private final PhysicalCardRepository physicalCardRepository;
	private final RetrievalCommandRepository commandRepository;
	private final ScanImageStorageService imageStorageService;
	private final ScryfallService scryfallService;

	public InventoryService(CardMetadataRepository metadataRepository,
			PhysicalCardRepository physicalCardRepository,
			RetrievalCommandRepository commandRepository,
			ScanImageStorageService imageStorageService,
			ScryfallService scryfallService) {
		this.metadataRepository = metadataRepository;
		this.physicalCardRepository = physicalCardRepository;
		this.commandRepository = commandRepository;
		this.imageStorageService = imageStorageService;
		this.scryfallService = scryfallService;
	}

	/** Legt pro Pi-UUID genau einen Pending-Datensatz an und lagert das Bild lokal aus. */
	@Transactional
	public InventoryCardResponse receiveScan(ScanSubmissionRequest request, MultipartFile image) {
		if (physicalCardRepository.existsById(request.uuid())) {
			throw new ApiException(HttpStatus.CONFLICT, "Diese Scan-UUID ist bereits vorhanden.");
		}
		String imagePath = imageStorageService.store(request.uuid(), image);
		PhysicalCard card = new PhysicalCard(
				request.uuid(),
				request.rackNumber().trim(),
				request.boxNumber().trim(),
				normalizeOptionalText(request.recognizedName()),
				imagePath);
		return toResponse(physicalCardRepository.save(card));
	}

	/** Liefert alle physischen Karten oder filtert nach Status und OCR-/Kartennamen. */
	@Transactional(readOnly = true)
	public List<InventoryCardResponse> listCards(CardStatus status, String query) {
		String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
		return physicalCardRepository.findAllByOrderByScannedAtDesc().stream()
				.filter(card -> status == null || card.getStatus() == status)
				.filter(card -> normalizedQuery.isEmpty()
						|| contains(card.getRecognizedName(), normalizedQuery)
						|| (card.getMetadata() != null && contains(card.getMetadata().getName(), normalizedQuery)))
				.map(this::toResponse)
				.toList();
	}

	/** Gibt eine Karte inklusive internem Bildpfad für den geschützten Bild-Endpunkt zurück. */
	@Transactional(readOnly = true)
	public PhysicalCard getPhysicalCard(UUID id) {
		return physicalCardRepository.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Der Scan wurde nicht gefunden."));
	}

	/** Verknüpft den Scan mit Scryfall-Metadaten und verwendet vorhandene Metadaten wieder. */
	@Transactional
	public InventoryCardResponse confirmScan(UUID id, UUID scryfallId) {
		PhysicalCard card = getPhysicalCard(id);
		if (card.getStatus() != CardStatus.PENDING) {
			throw new ApiException(HttpStatus.CONFLICT, "Nur noch nicht bestätigte Scans können abgeglichen werden.");
		}
		CardCandidateResponse selected = scryfallService.getById(scryfallId);
		CardMetadata metadata = metadataRepository.findById(selected.id())
				.orElseGet(() -> metadataRepository.save(new CardMetadata(
						selected.id(),
						selected.name(),
						selected.setCode(),
						selected.language(),
						selected.imageUrl())));
		card.confirm(metadata);
		return toResponse(physicalCardRepository.save(card));
	}

	/** Erstellt einen dauerhaft gespeicherten Auftrag für ein bestätigtes, noch eingelagertes Exemplar. */
	@Transactional
	public RetrievalCommandResponse requestRetrieval(UUID cardId) {
		PhysicalCard card = getPhysicalCard(cardId);
		if (card.getStatus() != CardStatus.CONFIRMED) {
			throw new ApiException(HttpStatus.CONFLICT, "Nur bestätigte und eingelagerte Karten können ausgelagert werden.");
		}
		card.requestRetrieval();
		physicalCardRepository.save(card);
		return toResponse(commandRepository.save(new RetrievalCommand(card)));
	}

	/** Listet offene Aufträge chronologisch, damit die Pi-Software sie wiederholbar abholen kann. */
	@Transactional(readOnly = true)
	public List<RetrievalCommandResponse> getQueuedCommands() {
		return commandRepository.findAllByStatusOrderByCreatedAtAsc(CommandStatus.QUEUED)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	/** Quittiert einen Befehl nach erfolgreicher physischer Auslagerung; wiederholte Quittungen sind idempotent. */
	@Transactional
	public RetrievalCommandResponse completeRetrieval(UUID commandId) {
		RetrievalCommand command = commandRepository.findById(commandId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Der Auslagerungsauftrag wurde nicht gefunden."));
		if (command.getStatus() == CommandStatus.QUEUED) {
			command.complete();
			command.getPhysicalCard().markRetrieved();
			commandRepository.save(command);
			physicalCardRepository.save(command.getPhysicalCard());
		}
		return toResponse(command);
	}

	private InventoryCardResponse toResponse(PhysicalCard card) {
		CardMetadata metadata = card.getMetadata();
		return new InventoryCardResponse(
				card.getId(),
				card.getRackNumber(),
				card.getBoxNumber(),
				card.getRecognizedName(),
				metadata == null ? null : metadata.getName(),
				metadata == null ? null : metadata.getSetCode(),
				metadata == null ? null : metadata.getLanguage(),
				metadata == null ? null : metadata.getImageUrl(),
				"/api/scans/" + card.getId() + "/image",
				card.getStatus(),
				card.getScannedAt());
	}

	private RetrievalCommandResponse toResponse(RetrievalCommand command) {
		PhysicalCard card = command.getPhysicalCard();
		return new RetrievalCommandResponse(
				command.getId(),
				card.getId(),
				card.getRackNumber(),
				card.getBoxNumber(),
				command.getStatus(),
				command.getCreatedAt());
	}

	private String normalizeOptionalText(String text) {
		return text == null || text.isBlank() ? null : text.trim();
	}

	private boolean contains(String value, String query) {
		return value != null && value.toLowerCase(Locale.ROOT).contains(query);
	}
}
