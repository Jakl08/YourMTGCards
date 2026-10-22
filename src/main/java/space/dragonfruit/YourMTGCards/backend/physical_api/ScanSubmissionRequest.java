package space.dragonfruit.YourMTGCards.backend.physical_api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Von der Pi-Software gelieferte Kennung, Regalposition und optionaler OCR-Text. */
public record ScanSubmissionRequest(
		@NotNull UUID uuid,
		@NotBlank String rackNumber,
		@NotBlank String boxNumber,
		String recognizedName) {
}
