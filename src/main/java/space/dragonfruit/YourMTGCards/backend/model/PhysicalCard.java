package space.dragonfruit.YourMTGCards.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Einzelnes physisches Exemplar mit Regalposition, Scanbild und optional bestätigten Metadaten. */
@Entity
@Table(name = "physical_inventory")
public class PhysicalCard {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "card_metadata_id")
	private CardMetadata metadata;

	@Column(name = "rack_number", nullable = false, length = 100)
	private String rackNumber;

	@Column(name = "box_number", nullable = false, length = 100)
	private String boxNumber;

	@Column(name = "recognized_name", length = 300)
	private String recognizedName;

	@Column(name = "scanned_image_path", nullable = false, length = 1000)
	private String scannedImagePath;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private CardStatus status;

	@Column(name = "scanned_at", nullable = false)
	private Instant scannedAt;

	protected PhysicalCard() {
		// JPA benötigt den Konstruktor für das Wiederherstellen gespeicherter Scans.
	}

	public PhysicalCard(UUID id, String rackNumber, String boxNumber, String recognizedName,
			String scannedImagePath) {
		this.id = id;
		this.rackNumber = rackNumber;
		this.boxNumber = boxNumber;
		this.recognizedName = recognizedName;
		this.scannedImagePath = scannedImagePath;
		this.status = CardStatus.PENDING;
		this.scannedAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public CardMetadata getMetadata() {
		return metadata;
	}

	public String getRackNumber() {
		return rackNumber;
	}

	public String getBoxNumber() {
		return boxNumber;
	}

	public String getRecognizedName() {
		return recognizedName;
	}

	public String getScannedImagePath() {
		return scannedImagePath;
	}

	public CardStatus getStatus() {
		return status;
	}

	public Instant getScannedAt() {
		return scannedAt;
	}

	public void confirm(CardMetadata metadata) {
		this.metadata = metadata;
		this.status = CardStatus.CONFIRMED;
	}

	public void requestRetrieval() {
		this.status = CardStatus.RETRIEVAL_REQUESTED;
	}

	public void markRetrieved() {
		this.status = CardStatus.RETRIEVED;
	}
}
