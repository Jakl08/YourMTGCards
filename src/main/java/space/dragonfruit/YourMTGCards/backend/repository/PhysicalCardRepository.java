package space.dragonfruit.YourMTGCards.backend.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import space.dragonfruit.YourMTGCards.backend.model.CardStatus;
import space.dragonfruit.YourMTGCards.backend.model.PhysicalCard;

/** Datenzugriff für physische Exemplare inklusive statusbasierter Listen. */
public interface PhysicalCardRepository extends JpaRepository<PhysicalCard, UUID> {

	List<PhysicalCard> findAllByStatusOrderByScannedAtDesc(CardStatus status);

	List<PhysicalCard> findAllByOrderByScannedAtDesc();

	List<PhysicalCard> findByRecognizedNameContainingIgnoreCaseOrMetadata_NameContainingIgnoreCaseOrderByScannedAtDesc(
			String recognizedName, String metadataName);
}
