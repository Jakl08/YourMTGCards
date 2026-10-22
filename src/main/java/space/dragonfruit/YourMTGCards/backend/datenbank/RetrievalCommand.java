package space.dragonfruit.YourMTGCards.backend.datenbank;

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

/** Persistenter Befehl, den die Pi-Software abholen und nach physischer Auslagerung quittieren kann. */
@Entity
@Table(name = "retrieval_commands")
public class RetrievalCommand {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "physical_card_id", nullable = false)
	private PhysicalCard physicalCard;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CommandStatus status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	protected RetrievalCommand() {
		// JPA benötigt den Konstruktor zum Laden von Aufträgen aus der Datenbank.
	}

	public RetrievalCommand(PhysicalCard physicalCard) {
		this.id = UUID.randomUUID();
		this.physicalCard = physicalCard;
		this.status = CommandStatus.QUEUED;
		this.createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public PhysicalCard getPhysicalCard() {
		return physicalCard;
	}

	public CommandStatus getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getCompletedAt() {
		return completedAt;
	}

	public void complete() {
		this.status = CommandStatus.COMPLETED;
		this.completedAt = Instant.now();
	}
}
