package space.dragonfruit.YourMTGCards.backend.dto;

import java.time.Instant;
import java.util.UUID;
import space.dragonfruit.YourMTGCards.backend.model.CommandStatus;

/** Hardware-Protokollantwort mit der Befehl-ID und der konkreten Regalposition. */
public record RetrievalCommandResponse(
		UUID commandId,
		UUID cardUuid,
		String rackNumber,
		String boxNumber,
		CommandStatus status,
		Instant createdAt) {
}
