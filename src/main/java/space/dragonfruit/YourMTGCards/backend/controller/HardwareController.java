package space.dragonfruit.YourMTGCards.backend.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import space.dragonfruit.YourMTGCards.backend.dto.InventoryCardResponse;
import space.dragonfruit.YourMTGCards.backend.dto.RetrievalCommandResponse;
import space.dragonfruit.YourMTGCards.backend.dto.ScanSubmissionRequest;
import space.dragonfruit.YourMTGCards.backend.service.InventoryService;

/** HTTP-Schnittstelle für den Pi: Scans hochladen, Aufträge abholen und Auslagerungen quittieren. */
@RestController
@RequestMapping("/api/hardware")
public class HardwareController {

	private final InventoryService inventoryService;

	public HardwareController(InventoryService inventoryService) {
		this.inventoryService = inventoryService;
	}

	/** Nimmt Metadaten als JSON-Teil und das JPEG-/PNG-Bild als Datei-Teil eines Multipart-Requests an. */
	@PostMapping(path = "/scans", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<InventoryCardResponse> receiveScan(
			@Valid @RequestPart("scan") ScanSubmissionRequest request,
			@RequestPart("image") MultipartFile image) {
		return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.receiveScan(request, image));
	}

	/** Liefert alle noch nicht quittierten Regalbefehle, die die Pi-Software ausführen muss. */
	@GetMapping("/retrieval-commands")
	public List<RetrievalCommandResponse> getRetrievalCommands() {
		return inventoryService.getQueuedCommands();
	}

	/** Bestätigt, dass die Hardware den benannten Auftrag physisch ausgeführt hat. */
	@PostMapping("/retrieval-commands/{commandId}/complete")
	public RetrievalCommandResponse completeRetrieval(@PathVariable UUID commandId) {
		return inventoryService.completeRetrieval(commandId);
	}
}
