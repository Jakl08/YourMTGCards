package space.dragonfruit.YourMTGCards.backend.datenbank;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Datenzugriff für physische Exemplare inklusive statusbasierter Listen. */
public interface PhysicalCardRepository extends JpaRepository<PhysicalCard, UUID> {

	List<PhysicalCard> findAllByStatusOrderByScannedAtDesc(CardStatus status);

	List<PhysicalCard> findAllByOrderByScannedAtDesc();

	List<PhysicalCard> findByRecognizedNameContainingIgnoreCaseOrMetadata_NameContainingIgnoreCaseOrderByScannedAtDesc(
			String recognizedName, String metadataName);
}
